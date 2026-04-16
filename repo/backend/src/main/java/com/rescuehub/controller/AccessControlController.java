package com.rescuehub.controller;

import com.rescuehub.entity.AccessListEntry;
import com.rescuehub.entity.User;
import com.rescuehub.security.SecurityUtils;
import com.rescuehub.service.AccessControlService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Admin-only endpoints for managing the org's allowlist and denylist.
 * All mutations require ADMIN role (enforced in AccessControlService).
 */
@RestController
@RequestMapping("/api/admin/access-control")
public class AccessControlController {

    private final AccessControlService accessControlService;

    public AccessControlController(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    record AddEntryRequest(
            @NotBlank String listType,
            @NotBlank String subjectType,
            @NotBlank String subjectValue,
            String reason,
            Instant expiresAt) {}

    @GetMapping
    public Map<String, Object> list() {
        User actor = SecurityUtils.currentUser();
        List<AccessListEntry> entries = accessControlService.listEntries(actor);
        return ApiResponse.data(entries);
    }

    @PostMapping
    public Map<String, Object> add(@Valid @RequestBody AddEntryRequest req) {
        User actor = SecurityUtils.currentUser();
        AccessListEntry entry = accessControlService.addEntry(actor,
                req.listType(), req.subjectType(), req.subjectValue(),
                req.reason(), req.expiresAt());
        return ApiResponse.data(entry);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> remove(@PathVariable Long id) {
        User actor = SecurityUtils.currentUser();
        accessControlService.removeEntry(actor, id);
        return ApiResponse.data(null);
    }
}
