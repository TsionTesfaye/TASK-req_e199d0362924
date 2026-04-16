package com.rescuehub.controller;

import com.rescuehub.entity.User;
import com.rescuehub.enums.Role;
import com.rescuehub.repository.AuditLogRepository;
import com.rescuehub.security.RoleGuard;
import com.rescuehub.security.SecurityUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditLogRepository auditLogRepo;
    private final RoleGuard roleGuard;

    public AuditController(AuditLogRepository auditLogRepo, RoleGuard roleGuard) {
        this.auditLogRepo = auditLogRepo;
        this.roleGuard = roleGuard;
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     @RequestParam(required = false) String q) {
        User actor = SecurityUtils.currentUser();
        roleGuard.require(actor, Role.ADMIN);
        String search = (q != null && !q.isBlank()) ? q.strip() : null;
        var logs = auditLogRepo.findFiltered(actor.getOrganizationId(), search, PageBounds.of(page, size));
        return ApiResponse.list(logs.getContent(), logs.getTotalElements(), page, size);
    }
}
