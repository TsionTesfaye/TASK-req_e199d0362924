package com.rescuehub.controller;

import com.rescuehub.entity.User;
import com.rescuehub.enums.Role;
import com.rescuehub.security.RoleGuard;
import com.rescuehub.security.SecurityUtils;
import com.rescuehub.service.RetentionService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/retention")
public class RetentionController {

    private final RetentionService retentionService;
    private final RoleGuard roleGuard;

    public RetentionController(RetentionService retentionService, RoleGuard roleGuard) {
        this.retentionService = retentionService;
        this.roleGuard = roleGuard;
    }

    @PostMapping("/archive-run")
    public Map<String, Object> archiveRun() {
        User actor = SecurityUtils.currentUser();
        roleGuard.require(actor, Role.ADMIN);
        int count = retentionService.archiveEligible(actor.getOrganizationId());
        return ApiResponse.data(ApiResponse.safeMap("archivedCount", count));
    }
}
