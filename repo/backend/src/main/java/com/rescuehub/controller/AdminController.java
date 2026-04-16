package com.rescuehub.controller;

import com.rescuehub.entity.User;
import com.rescuehub.enums.Role;
import com.rescuehub.security.SecurityUtils;
import com.rescuehub.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    record CreateUserRequest(@NotBlank String username, @NotBlank String password,
                              @NotBlank String displayName, @NotNull Role role) {}
    record UpdateUserRequest(@NotBlank String displayName, @NotNull Role role,
                              boolean isActive, boolean isFrozen) {}

    @PostMapping("/users")
    public Map<String, Object> createUser(@Valid @RequestBody CreateUserRequest req, HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(adminService.createUser(actor, req.username(), req.password(),
                req.displayName(), req.role(), request.getRemoteAddr(), SecurityUtils.currentWorkstationId()));
    }

    @GetMapping("/users")
    public Map<String, Object> listUsers(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        User actor = SecurityUtils.currentUser();
        var users = adminService.listUsers(actor, PageBounds.of(page, size));
        return ApiResponse.list(users.getContent(), users.getTotalElements(), page, size);
    }

    @GetMapping("/users/{id}")
    public Map<String, Object> getUser(@PathVariable Long id) {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(adminService.getUser(actor, id));
    }

    @PutMapping("/users/{id}")
    public Map<String, Object> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest req,
                                           HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(adminService.updateUser(actor, id, req.displayName(), req.role(),
                req.isActive(), req.isFrozen(), request.getRemoteAddr(), SecurityUtils.currentWorkstationId()));
    }

    @DeleteMapping("/users/{id}")
    public Map<String, Object> deleteUser(@PathVariable Long id, HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        adminService.deleteUser(actor, id, request.getRemoteAddr(), SecurityUtils.currentWorkstationId());
        return ApiResponse.data(null);
    }
}
