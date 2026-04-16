package com.rescuehub.service;

import com.rescuehub.entity.CorrectiveAction;
import com.rescuehub.entity.User;
import com.rescuehub.enums.CorrectiveActionStatus;
import com.rescuehub.enums.Role;
import com.rescuehub.exception.BusinessRuleException;
import com.rescuehub.exception.NotFoundException;
import com.rescuehub.repository.CorrectiveActionRepository;
import com.rescuehub.security.RoleGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;

@Service
public class CorrectiveActionService {

    private static final Map<CorrectiveActionStatus, EnumSet<CorrectiveActionStatus>> TRANSITIONS = Map.of(
            CorrectiveActionStatus.OPEN, EnumSet.of(CorrectiveActionStatus.ASSIGNED),
            CorrectiveActionStatus.ASSIGNED, EnumSet.of(CorrectiveActionStatus.IN_PROGRESS),
            CorrectiveActionStatus.IN_PROGRESS, EnumSet.of(CorrectiveActionStatus.RESOLVED),
            CorrectiveActionStatus.RESOLVED, EnumSet.of(CorrectiveActionStatus.VERIFIED_CLOSED),
            CorrectiveActionStatus.VERIFIED_CLOSED, EnumSet.noneOf(CorrectiveActionStatus.class)
    );

    private final CorrectiveActionRepository repo;
    private final AuditService auditService;
    private final RoleGuard roleGuard;

    public CorrectiveActionService(CorrectiveActionRepository repo, AuditService auditService, RoleGuard roleGuard) {
        this.repo = repo;
        this.auditService = auditService;
        this.roleGuard = roleGuard;
    }

    @Transactional
    public CorrectiveAction create(User actor, String description, Long relatedVisitId, Long relatedRuleResultId,
                                   String ip, String workstationId) {
        roleGuard.require(actor, Role.QUALITY, Role.ADMIN);
        CorrectiveAction ca = new CorrectiveAction();
        ca.setOrganizationId(actor.getOrganizationId());
        ca.setDescription(description);
        ca.setRelatedVisitId(relatedVisitId);
        ca.setRelatedRuleResultId(relatedRuleResultId);
        ca.setStatus(CorrectiveActionStatus.OPEN);
        ca = repo.save(ca);
        auditService.log(actor.getId(), actor.getUsername(), "CORRECTIVE_ACTION_CREATE",
                "CorrectiveAction", String.valueOf(ca.getId()), actor.getOrganizationId(), ip, workstationId, null, null);
        return ca;
    }

    @Transactional
    public CorrectiveAction transition(User actor, Long id, CorrectiveActionStatus newStatus,
                                       String resolutionNote, Long assignedTo, String ip, String workstationId) {
        roleGuard.require(actor, Role.QUALITY, Role.ADMIN);
        CorrectiveAction ca = repo.findByOrganizationIdAndId(actor.getOrganizationId(), id)
                .orElseThrow(() -> new NotFoundException("Corrective action not found"));

        EnumSet<CorrectiveActionStatus> allowed = TRANSITIONS.getOrDefault(ca.getStatus(), EnumSet.noneOf(CorrectiveActionStatus.class));
        if (!allowed.contains(newStatus)) {
            throw new BusinessRuleException("Invalid transition from " + ca.getStatus() + " to " + newStatus);
        }

        String before = "{\"status\":\"" + ca.getStatus() + "\"}";
        ca.setStatus(newStatus);
        if (assignedTo != null) ca.setAssignedToUserId(assignedTo);
        if (resolutionNote != null) ca.setResolutionNote(resolutionNote);
        if (newStatus == CorrectiveActionStatus.VERIFIED_CLOSED) ca.setClosedAt(Instant.now());
        ca = repo.save(ca);

        auditService.log(actor.getId(), actor.getUsername(), "CORRECTIVE_ACTION_TRANSITION",
                "CorrectiveAction", String.valueOf(id), actor.getOrganizationId(), ip, workstationId,
                before, "{\"status\":\"" + newStatus + "\"}");
        return ca;
    }

    @Transactional(readOnly = true)
    public Page<CorrectiveAction> list(User actor, Pageable pageable) {
        roleGuard.require(actor, Role.QUALITY, Role.ADMIN);
        return repo.findByOrganizationId(actor.getOrganizationId(), pageable);
    }

    @Transactional(readOnly = true)
    public CorrectiveAction getById(User actor, Long id) {
        roleGuard.require(actor, Role.QUALITY, Role.ADMIN);
        return repo.findByOrganizationIdAndId(actor.getOrganizationId(), id)
                .orElseThrow(() -> new NotFoundException("Corrective action not found"));
    }
}
