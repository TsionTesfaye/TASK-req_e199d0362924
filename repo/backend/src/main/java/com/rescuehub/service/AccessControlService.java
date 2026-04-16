package com.rescuehub.service;

import com.rescuehub.entity.AccessListEntry;
import com.rescuehub.entity.User;
import com.rescuehub.enums.Role;
import com.rescuehub.exception.ForbiddenException;
import com.rescuehub.repository.AccessListEntryRepository;
import com.rescuehub.security.RoleGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Manages per-organization allowlists and denylists enforced at login,
 * export, and other sensitive operations.
 *
 * Decision logic (DENY wins):
 *  1. If any active DENY entry matches → DENIED (audit + reject).
 *  2. If the org has any active ALLOW entries for this subject_type, and none match → DENIED.
 *  3. Otherwise → allowed (pass-through).
 */
@Service
public class AccessControlService {

    public enum Decision { ALLOWED, DENIED }

    private final AccessListEntryRepository repo;
    private final AuditService auditService;
    private final RoleGuard roleGuard;

    public AccessControlService(AccessListEntryRepository repo, AuditService auditService,
                                 RoleGuard roleGuard) {
        this.repo = repo;
        this.auditService = auditService;
        this.roleGuard = roleGuard;
    }

    /**
     * Check a subject against the org's access lists.
     *
     * @param orgId       organization being accessed (nullable for pre-auth checks — uses global entries only)
     * @param subjectType IP | USERNAME | WORKSTATION_ID
     * @param value       the value to check
     * @param actorDesc   description used in audit log (e.g. "anonymous" before login)
     * @param ip          requester IP for audit
     */
    @Transactional(readOnly = true)
    public Decision check(Long orgId, String subjectType, String value,
                          String actorDesc, String ip) {
        List<AccessListEntry> entries = orgId != null
                ? repo.findActive(orgId, subjectType, value, Instant.now())
                : repo.findActive(null, subjectType, value, Instant.now())
                      .stream()
                      .filter(e -> e.getOrganizationId() == null)
                      .toList();

        // 1. Explicit deny
        boolean denied = entries.stream().anyMatch(e -> "DENY".equals(e.getListType()));
        if (denied) {
            auditService.log(null, actorDesc, "ACCESS_DENIED",
                    subjectType, value, orgId, ip, null, null,
                    "{\"reason\":\"denylist\",\"subjectType\":\"" + subjectType
                            + "\",\"value\":\"" + value + "\"}");
            return Decision.DENIED;
        }

        // 2. If allowlist is non-empty and value not in it, deny
        // Query all ALLOW entries for this org + subject_type (regardless of value)
        boolean allowlistActive = hasAllowlistEntries(orgId, subjectType);
        boolean inAllowlist = entries.stream().anyMatch(e -> "ALLOW".equals(e.getListType()));
        if (allowlistActive && !inAllowlist) {
            auditService.log(null, actorDesc, "ACCESS_DENIED",
                    subjectType, value, orgId, ip, null, null,
                    "{\"reason\":\"not_on_allowlist\",\"subjectType\":\"" + subjectType
                            + "\",\"value\":\"" + value + "\"}");
            return Decision.DENIED;
        }

        if (inAllowlist) {
            auditService.log(null, actorDesc, "ACCESS_ALLOWED",
                    subjectType, value, orgId, ip, null, null,
                    "{\"reason\":\"allowlist\",\"subjectType\":\"" + subjectType + "\"}");
        }
        return Decision.ALLOWED;
    }

    @Transactional
    public AccessListEntry addEntry(User actor, String listType, String subjectType,
                                    String subjectValue, String reason, Instant expiresAt) {
        roleGuard.require(actor, Role.ADMIN);
        if (!"ALLOW".equals(listType) && !"DENY".equals(listType)) {
            throw new com.rescuehub.exception.BusinessRuleException("listType must be ALLOW or DENY");
        }
        if (!List.of("IP", "USERNAME", "WORKSTATION_ID").contains(subjectType)) {
            throw new com.rescuehub.exception.BusinessRuleException(
                    "subjectType must be IP, USERNAME, or WORKSTATION_ID");
        }
        AccessListEntry entry = new AccessListEntry();
        entry.setOrganizationId(actor.getOrganizationId());
        entry.setListType(listType);
        entry.setSubjectType(subjectType);
        entry.setSubjectValue(subjectValue);
        entry.setReason(reason);
        entry.setCreatedByUserId(actor.getId());
        entry.setExpiresAt(expiresAt);
        entry = repo.save(entry);
        auditService.log(actor.getId(), actor.getUsername(), "ACCESS_LIST_ENTRY_ADDED",
                "AccessListEntry", String.valueOf(entry.getId()),
                actor.getOrganizationId(), null, null, null,
                "{\"listType\":\"" + listType + "\",\"subjectType\":\"" + subjectType
                        + "\",\"value\":\"" + subjectValue + "\"}");
        return entry;
    }

    @Transactional
    public void removeEntry(User actor, Long entryId) {
        roleGuard.require(actor, Role.ADMIN);
        AccessListEntry entry = repo.findById(entryId)
                .orElseThrow(() -> new com.rescuehub.exception.NotFoundException("Entry not found"));
        if (!entry.getOrganizationId().equals(actor.getOrganizationId())) {
            throw new ForbiddenException("Entry not in your organization");
        }
        repo.delete(entry);
        auditService.log(actor.getId(), actor.getUsername(), "ACCESS_LIST_ENTRY_REMOVED",
                "AccessListEntry", String.valueOf(entryId),
                actor.getOrganizationId(), null, null, null, null);
    }

    @Transactional(readOnly = true)
    public List<AccessListEntry> listEntries(User actor) {
        roleGuard.require(actor, Role.ADMIN);
        return repo.findByOrganizationId(actor.getOrganizationId());
    }

    private boolean hasAllowlistEntries(Long orgId, String subjectType) {
        if (orgId == null) return false;
        return repo.findByOrganizationId(orgId).stream()
                .anyMatch(e -> "ALLOW".equals(e.getListType())
                        && subjectType.equals(e.getSubjectType())
                        && (e.getExpiresAt() == null || e.getExpiresAt().isAfter(Instant.now())));
    }
}
