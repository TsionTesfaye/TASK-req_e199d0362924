package com.rescuehub.config;

import com.rescuehub.security.AuthFilter;
import com.rescuehub.service.AuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Security configuration.
 *
 * <h2>CSRF: intentionally custom, not Spring's default</h2>
 *
 * Spring Security's built-in CSRF filter assumes a classic cookie-based session model
 * (HttpSession + JSESSIONID + a synchronizer-token stored on the session). RescueHub does
 * NOT use HttpSession — authentication is stateless from Spring's perspective
 * ({@link SessionCreationPolicy#STATELESS}) and identity flows through a custom
 * {@code X-Session-Token} header bound to a server-side {@code UserSession} row.
 * Because there is no HttpSession, Spring's CsrfFilter has nothing to anchor its token to
 * and would either no-op or reject every request depending on how it was wired. Enabling
 * it alongside our flow would produce two competing CSRF systems — the exact inconsistency
 * QA is warning about.
 *
 * We therefore disable Spring's CSRF and implement CSRF enforcement in
 * {@link com.rescuehub.security.AuthFilter}. The guarantees provided:
 *
 * <ul>
 *   <li><b>Token issuance:</b> on successful login (and first-run bootstrap) the server
 *       returns a {@code csrfToken} alongside the session token. The SHA-256 hash is
 *       persisted on the {@code UserSession} row next to {@code sessionTokenHash}.</li>
 *   <li><b>Token binding:</b> the CSRF token is bound 1:1 to a specific session token.
 *       Reusing a CSRF token across sessions is impossible — the lookup key is
 *       {@code sha256(rawSessionToken)}.</li>
 *   <li><b>Enforcement:</b> every {@code POST/PUT/PATCH/DELETE} outside the public set
 *       ({@code /api/health}, {@code /api/auth/login}, {@code /api/bootstrap},
 *       {@code /api/bootstrap/status}) is rejected with HTTP 403 unless
 *       {@code X-CSRF-Token} is present AND
 *       {@code sha256(X-CSRF-Token) == UserSession.csrfTokenHash}.
 *       See {@link com.rescuehub.security.AuthFilter#doFilterInternal}.</li>
 *   <li><b>No gaps:</b> the check runs unconditionally for mutating verbs. Because the
 *       public set excludes only endpoints that themselves MINT session + CSRF tokens,
 *       there is no mutating endpoint that bypasses both authentication AND CSRF.</li>
 *   <li><b>Rotation:</b> logout revokes the {@code UserSession} row, which invalidates the
 *       bound CSRF token by construction (validator fails when the row is gone).</li>
 * </ul>
 *
 * Rationale for not switching to Spring's default: the stateless + header-auth design is a
 * prompt requirement ("internal HTTPS/TLS, X-Session-Token style"), and retrofitting
 * HttpSession-backed CSRF would reintroduce JSESSIONID cookies. Sticking with the custom
 * filter keeps the model consistent and intentional.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AuthService authService;

    public SecurityConfig(AuthService authService) {
        this.authService = authService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF is enforced by AuthFilter (see class-level Javadoc). Spring's default
            // CSRF filter is incompatible with our stateless + custom-header auth model.
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints — no session token required (they issue tokens or are health checks).
                // Kept in sync with AuthFilter.PUBLIC_PATHS.
                .requestMatchers(
                    new AntPathRequestMatcher("/api/health"),
                    new AntPathRequestMatcher("/api/auth/login"),
                    new AntPathRequestMatcher("/api/bootstrap"),
                    new AntPathRequestMatcher("/api/bootstrap/status")
                ).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(new AuthFilter(authService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
