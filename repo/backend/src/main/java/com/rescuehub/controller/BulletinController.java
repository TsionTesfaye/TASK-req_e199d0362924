package com.rescuehub.controller;

import com.rescuehub.entity.User;
import com.rescuehub.enums.BulletinStatus;
import com.rescuehub.security.SecurityUtils;
import com.rescuehub.service.BulletinService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/bulletins")
public class BulletinController {

    private final BulletinService bulletinService;

    public BulletinController(BulletinService bulletinService) {
        this.bulletinService = bulletinService;
    }

    record CreateRequest(@NotBlank String title, @NotBlank String body) {}
    record StatusRequest(@NotBlank String status) {}

    @PostMapping
    public Map<String, Object> create(@Valid @RequestBody CreateRequest req, HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(bulletinService.create(actor, req.title(), req.body(),
                request.getRemoteAddr(), SecurityUtils.currentWorkstationId()));
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        User actor = SecurityUtils.currentUser();
        var bulletins = bulletinService.list(actor, PageBounds.of(page, size));
        return ApiResponse.list(bulletins.getContent(), bulletins.getTotalElements(), page, size);
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Long id) {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(bulletinService.getById(actor, id));
    }

    @PostMapping("/{id}/status")
    public Map<String, Object> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req,
                                             HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        BulletinStatus status = BulletinStatus.valueOf(req.status().toUpperCase());
        return ApiResponse.data(bulletinService.updateStatus(actor, id, status,
                request.getRemoteAddr(), SecurityUtils.currentWorkstationId()));
    }
}
