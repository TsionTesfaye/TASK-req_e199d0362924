package com.rescuehub.controller;

import com.rescuehub.entity.User;
import com.rescuehub.repository.UserRepository;
import com.rescuehub.service.AuthService;
import com.rescuehub.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepo;

    public AuthController(AuthService authService, UserRepository userRepo) {
        this.authService = authService;
        this.userRepo = userRepo;
    }

    record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");
        AuthService.LoginResult result = authService.loginWithCsrf(
                req.username(), req.password(), ip, ua != null ? ua : "");
        User user = userRepo.findByUsername(req.username()).orElseThrow();
        return ApiResponse.data(ApiResponse.safeMap(
                "sessionToken", result.sessionToken(),
                "csrfToken", result.csrfToken(),
                "user", userPayload(user)
        ));
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(@RequestHeader("X-Session-Token") String token, HttpServletRequest request) {
        authService.logout(token, request.getRemoteAddr(), request.getHeader("User-Agent"));
        return ApiResponse.data(ApiResponse.safeMap("status", "logged out"));
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        User user = SecurityUtils.currentUser();
        return ApiResponse.data(userPayload(user));
    }

    private static Map<String, Object> userPayload(User user) {
        return ApiResponse.safeMap(
                "id", user.getId(),
                "username", user.getUsername(),
                "displayName", user.getDisplayName(),
                "role", user.getRole() != null ? user.getRole().name() : null,
                "organizationId", user.getOrganizationId()
        );
    }
}
