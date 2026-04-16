package com.rescuehub.security;

import com.rescuehub.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Set;

public class AuthFilter extends OncePerRequestFilter {

    /** Public paths — no auth and no CSRF required. Visible to tests so the CSRF-coverage
     *  assertion cannot drift out of sync with runtime behavior. */
    public static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/health", "/api/auth/login",
            "/api/bootstrap", "/api/bootstrap/status");
    public static final Set<String> MUTATING = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final AuthService authService;

    public AuthFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Public endpoints: no auth, no CSRF (bootstrap/login issue the tokens).
        if (PUBLIC_PATHS.contains(path)) {
            chain.doFilter(request, response);
            return;
        }

        String token = request.getHeader("X-Session-Token");
        if (token != null && !token.isBlank()) {
            try {
                AuthService.SessionContext ctx = authService.validateToken(token);
                SecurityContextHolder.getContext().setAuthentication(
                        new UserAuthentication(ctx.user(), ctx.workstationId()));
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }

        // CSRF enforcement: every mutating request must present a matching X-CSRF-Token.
        // Uploads (multipart) use the same header mechanism.
        if (MUTATING.contains(method)) {
            String csrf = request.getHeader("X-CSRF-Token");
            if (token == null || token.isBlank() || csrf == null || csrf.isBlank()
                    || !authService.validateCsrf(token, csrf)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"code\":403,\"message\":\"CSRF token missing or invalid\",\"details\":null}");
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
