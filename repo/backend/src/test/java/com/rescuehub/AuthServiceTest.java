package com.rescuehub;

import com.rescuehub.exception.AuthException;
import com.rescuehub.exception.RateLimitException;
import com.rescuehub.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceTest extends BaseIntegrationTest {

    @Autowired
    private AuthService authService;

    @Test
    void loginSuccess() {
        String token = authService.login("test_admin", "password", "127.0.0.1", "TestUA");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void loginFailure_wrongPassword() {
        assertThrows(AuthException.class, () ->
                authService.login("test_admin", "wrongpassword", "127.0.0.2", "TestUA"));
    }

    @Test
    void loginFailure_unknownUser() {
        assertThrows(AuthException.class, () ->
                authService.login("nonexistent_user_xyz", "password", "127.0.0.3", "TestUA"));
    }

    @Test
    void rateLimiting() {
        // Exhaust rate limit with wrong password
        String ip = "10.0.0.99";
        String ua = "RateLimitTestUA";
        // 10 failures should trigger rate limit on the 11th attempt
        for (int i = 0; i < 10; i++) {
            try {
                authService.login("nonexistent_user_xyz", "bad", ip, ua);
            } catch (AuthException ignore) {}
        }
        assertThrows(RateLimitException.class, () ->
                authService.login("test_admin", "password", ip, ua));
    }

    @Test
    void sessionValidation() {
        String token = authService.login("test_admin", "password", "127.0.0.1", "TestUA");
        AuthService.SessionContext ctx = authService.validateToken(token);
        assertNotNull(ctx);
        assertEquals("test_admin", ctx.user().getUsername());
    }

    @Test
    void logoutInvalidatesSession() {
        String token = authService.login("test_admin", "password", "127.0.0.1", "TestUA");
        authService.logout(token, "127.0.0.1", "TestUA");
        assertThrows(AuthException.class, () -> authService.validateToken(token));
    }

    @Test
    void loginWithCsrf_returnsBothTokens() {
        AuthService.LoginResult result = authService.loginWithCsrf("test_admin", "password", "127.0.0.1", "CsrfUA");
        assertNotNull(result);
        assertNotNull(result.sessionToken());
        assertNotNull(result.csrfToken());
        assertFalse(result.sessionToken().isBlank());
        assertFalse(result.csrfToken().isBlank());
    }

    @Test
    void validateCsrf_correctToken_returnsTrue() {
        AuthService.LoginResult result = authService.loginWithCsrf("test_admin", "password", "127.0.0.1", "CsrfUA2");
        assertTrue(authService.validateCsrf(result.sessionToken(), result.csrfToken()));
    }

    @Test
    void validateCsrf_wrongToken_returnsFalse() {
        AuthService.LoginResult result = authService.loginWithCsrf("test_admin", "password", "127.0.0.1", "CsrfUA3");
        assertFalse(authService.validateCsrf(result.sessionToken(), "definitely-wrong-csrf"));
    }

    @Test
    void validateCsrf_nullInputs_returnsFalse() {
        assertFalse(authService.validateCsrf(null, null));
        assertFalse(authService.validateCsrf("sometoken", null));
        assertFalse(authService.validateCsrf(null, "somecsrf"));
    }

    @Test
    void getPasswordEncoder_returnsNonNull() {
        assertNotNull(authService.getPasswordEncoder());
    }
}
