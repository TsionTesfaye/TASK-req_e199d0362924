package com.rescuehub.controller;

import com.rescuehub.service.SystemBootstrapService;
import com.rescuehub.service.SystemBootstrapService.BootstrapResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/bootstrap")
public class BootstrapController {

    private final SystemBootstrapService bootstrapService;

    public BootstrapController(SystemBootstrapService bootstrapService) {
        this.bootstrapService = bootstrapService;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return ApiResponse.data(ApiResponse.safeMap("initialized", bootstrapService.isSystemInitialized()));
    }

    public record BootstrapRequest(
            @NotBlank String username,
            @NotBlank String password,
            @NotBlank String confirmPassword,
            String displayName,
            String organizationName) {}

    @PostMapping
    public Map<String, Object> bootstrap(@Valid @RequestBody BootstrapRequest req, HttpServletRequest request) {
        if (!req.password().equals(req.confirmPassword())) {
            throw new com.rescuehub.exception.BusinessRuleException("password and confirmPassword do not match");
        }
        String ip = request.getRemoteAddr();
        String ua = request.getHeader("User-Agent");
        BootstrapResult result = bootstrapService.initializeSystem(
                req.username(), req.password(), req.displayName(), req.organizationName(),
                ip, ua != null ? ua : "");
        return ApiResponse.data(ApiResponse.safeMap(
                "userId", result.userId(),
                "organizationId", result.organizationId(),
                "sessionToken", result.sessionToken(),
                "csrfToken", result.csrfToken(),
                "user", ApiResponse.safeMap(
                        "id", result.user().id(),
                        "username", result.user().username(),
                        "displayName", result.user().displayName(),
                        "role", result.user().role(),
                        "organizationId", result.user().organizationId()
                )
        ));
    }
}
