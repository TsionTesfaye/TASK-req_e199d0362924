package com.rescuehub;

import com.rescuehub.security.AuthFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mechanical proof that the custom CSRF filter has no gaps.
 *
 * <p>For every mutating route ({@code POST/PUT/PATCH/DELETE}) that Spring discovers via
 * {@link RequestMappingHandlerMapping}, this test issues a real HTTP request with no
 * {@code X-CSRF-Token} and no {@code X-Session-Token} and asserts the response is
 * {@code 403}. Public token-minting endpoints and the health endpoint are excluded from
 * the check — they are the documented public set in
 * {@link com.rescuehub.security.AuthFilter#PUBLIC_PATHS}.
 *
 * <p>If any new controller adds a mutating endpoint and forgets to ensure CSRF coverage,
 * this test fails — converting the "no gaps" claim from prose into a build-time guarantee.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CsrfCoverageTest {

    /**
     * The canonical minimal set of paths that are legitimately public (no auth, no CSRF).
     * Defined here independently of the runtime allowlist so that a mistaken addition to
     * AuthFilter.PUBLIC_PATHS is caught rather than silently excluded from coverage checks.
     */
    private static final Set<String> EXPECTED_PUBLIC_PATHS = Set.of(
            "/api/health", "/api/auth/login",
            "/api/bootstrap", "/api/bootstrap/status");

    private static final Set<String> MUTATING = AuthFilter.MUTATING;

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @LocalServerPort
    private int port;

    /**
     * Guard: the runtime PUBLIC_PATHS must exactly match the expected minimal set.
     * Any deviation (extra entry or missing entry) is a security misconfiguration.
     */
    @Test
    void runtime_public_paths_match_expected_minimal_set() {
        assertEquals(EXPECTED_PUBLIC_PATHS, AuthFilter.PUBLIC_PATHS,
                "AuthFilter.PUBLIC_PATHS has drifted from the expected minimal set. "
                        + "Only health/login/bootstrap endpoints should be public. "
                        + "Extra entries bypass auth AND CSRF for all methods.");
    }

    @Test
    void every_mutating_endpoint_rejects_missing_csrf_token() throws Exception {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        List<String> checked = new ArrayList<>();
        List<String> violations = new ArrayList<>();

        for (Map.Entry<RequestMappingInfo, ?> e : handlerMapping.getHandlerMethods().entrySet()) {
            RequestMappingInfo info = e.getKey();
            Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
            if (methods.isEmpty()) continue;

            Set<String> patterns = extractPatterns(info);
            for (RequestMethod rm : methods) {
                if (!MUTATING.contains(rm.name())) continue;
                for (String pattern : patterns) {
                    if (!pattern.startsWith("/api/")) continue;
                    if (EXPECTED_PUBLIC_PATHS.contains(pattern)) continue;

                    // Replace path variables with dummy ids so the URL is syntactically valid.
                    String url = pattern.replaceAll("\\{[^}]+\\}", "1");
                    String target = "http://localhost:" + port + url;

                    HttpRequest req = HttpRequest.newBuilder(URI.create(target))
                            .method(rm.name(),
                                    HttpRequest.BodyPublishers.ofString("{}"))
                            .header("Content-Type", "application/json")
                            // deliberately NO X-CSRF-Token and NO X-Session-Token
                            .timeout(Duration.ofSeconds(10))
                            .build();

                    HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                    checked.add(rm.name() + " " + url + " -> " + resp.statusCode());

                    if (resp.statusCode() != 403) {
                        violations.add(rm.name() + " " + url
                                + " returned " + resp.statusCode() + " (expected 403)");
                    }
                    // Also assert the body carries the CSRF message — confirms it's the
                    // CSRF gate that rejected, not some other 403 path.
                    else if (!resp.body().contains("CSRF token missing or invalid")) {
                        violations.add(rm.name() + " " + url
                                + " returned 403 but not the CSRF-specific message: " + resp.body());
                    }
                }
            }
        }

        // Minimum floor — the app has dozens of mutating routes. If we find fewer than 20,
        // the handler-mapping scan silently missed something.
        assertTrue(checked.size() >= 20,
                "expected >= 20 mutating routes, only checked " + checked.size()
                        + ":\n  " + String.join("\n  ", checked));
        assertTrue(violations.isEmpty(),
                "CSRF coverage gaps detected:\n  " + String.join("\n  ", violations)
                        + "\nRoutes checked:\n  " + String.join("\n  ", checked));
        System.out.println("[CsrfCoverage] verified " + checked.size() + " mutating routes");
    }

    /**
     * Sanity: the filter MUST let health through (no CSRF required for GET) and MUST
     * accept bootstrap POST without CSRF (because bootstrap mints the first token).
     * If either of these starts returning 403, the public-path set is broken.
     */
    @Test
    void public_paths_do_not_require_csrf() throws Exception {
        HttpClient http = HttpClient.newHttpClient();
        HttpResponse<String> health = http.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/health"))
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, health.statusCode(), "GET /api/health must be public");

        // Bootstrap POST without CSRF token — may succeed or fail for business reasons,
        // but MUST NOT be rejected with the CSRF-specific 403 message.
        HttpResponse<String> boot = http.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/bootstrap"))
                        .POST(HttpRequest.BodyPublishers.ofString("{}"))
                        .header("Content-Type", "application/json")
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertFalse(boot.body().contains("CSRF token missing or invalid"),
                "bootstrap must not be gated by CSRF (it issues the first token)");
    }

    /**
     * Spring Boot 3 uses path-patterns by default but also supports legacy ant-style
     * patterns. Pull from whichever is populated.
     */
    private static Set<String> extractPatterns(RequestMappingInfo info) {
        Set<String> out = new HashSet<>();
        if (info.getPathPatternsCondition() != null) {
            info.getPathPatternsCondition().getPatterns()
                    .forEach(p -> out.add(p.getPatternString()));
        }
        if (info.getPatternsCondition() != null) {
            out.addAll(info.getPatternsCondition().getPatterns());
        }
        return out;
    }
}
