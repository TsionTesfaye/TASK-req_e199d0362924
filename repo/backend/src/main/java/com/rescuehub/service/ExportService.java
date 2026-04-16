package com.rescuehub.service;

import com.rescuehub.entity.LedgerEntry;
import com.rescuehub.entity.User;
import com.rescuehub.enums.Role;
import com.rescuehub.exception.BusinessRuleException;
import com.rescuehub.exception.ForbiddenException;
import com.rescuehub.repository.AuditLogRepository;
import com.rescuehub.repository.LedgerEntryRepository;
import com.rescuehub.repository.PatientRepository;
import com.rescuehub.security.RoleGuard;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;

@Service
public class ExportService {

    private static final int ELEVATION_THRESHOLD = 500;

    @Value("${rescuehub.storage.dir}")
    private String storageDir;

    private final LedgerEntryRepository ledgerRepo;
    private final AuditLogRepository auditLogRepo;
    private final PatientRepository patientRepo;
    private final IdempotencyService idempotencyService;
    private final AuditService auditService;
    private final RoleGuard roleGuard;
    private final RiskScoreService riskScoreService;

    private final AccessControlService accessControlService;

    public ExportService(LedgerEntryRepository ledgerRepo, AuditLogRepository auditLogRepo,
                         PatientRepository patientRepo, IdempotencyService idempotencyService,
                         AuditService auditService, RoleGuard roleGuard,
                         RiskScoreService riskScoreService,
                         AccessControlService accessControlService) {
        this.ledgerRepo = ledgerRepo;
        this.auditLogRepo = auditLogRepo;
        this.patientRepo = patientRepo;
        this.idempotencyService = idempotencyService;
        this.auditService = auditService;
        this.roleGuard = roleGuard;
        this.riskScoreService = riskScoreService;
        this.accessControlService = accessControlService;
    }

    @Transactional
    public String export(User actor, String exportType, String idempotencyKey,
                          boolean elevated, boolean secondConfirmation,
                          String ip, String workstationId) {
        roleGuard.require(actor, Role.BILLING, Role.ADMIN);
        // Allowlist / denylist enforcement on export (sensitive operation)
        if (accessControlService.check(actor.getOrganizationId(), "IP", ip,
                actor.getUsername(), ip) == AccessControlService.Decision.DENIED) {
            throw new com.rescuehub.exception.ForbiddenException("Export access denied by security policy");
        }

        // Every export attempt raises the risk score for the workstation.
        // Non-enforcing by design — surfaces to ADMIN via /api/risk-scores.
        riskScoreService.recordEvent(workstationId, "REPEATED_EXPORT");

        String cached = idempotencyService.checkAndReserve(
                actor.getOrganizationId(), actor.getId(), idempotencyKey, exportType);
        if (cached != null) return cached;

        // Count rows first
        long count = switch (exportType.toLowerCase()) {
            case "ledger" -> ledgerRepo.findByOrganizationId(actor.getOrganizationId(), PageRequest.of(0, 1)).getTotalElements();
            case "audit" -> auditLogRepo.findByOrganizationId(actor.getOrganizationId(), PageRequest.of(0, 1)).getTotalElements();
            case "patients" -> patientRepo.findByOrganizationId(actor.getOrganizationId(), PageRequest.of(0, 1)).getTotalElements();
            default -> throw new BusinessRuleException("Unknown export type: " + exportType);
        };

        if (count > ELEVATION_THRESHOLD && (!elevated || !secondConfirmation)) {
            throw new BusinessRuleException("Export of " + count + " rows requires elevated=true and secondConfirmation=true");
        }

        String filePath = writeExport(actor.getOrganizationId(), exportType, count);

        auditService.log(actor.getId(), actor.getUsername(), "EXPORT_CREATED",
                "Export", exportType, actor.getOrganizationId(), ip, workstationId,
                null, "{\"type\":\"" + exportType + "\",\"rows\":" + count + ",\"path\":\"" + filePath + "\"}");

        String snapshot = "{\"type\":\"" + exportType + "\",\"rows\":" + count + ",\"filePath\":\"" + filePath + "\"}";
        idempotencyService.complete(actor.getOrganizationId(), idempotencyKey, snapshot);
        return snapshot;
    }

    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> listHistory(User actor, Pageable pageable) {
        roleGuard.require(actor, Role.BILLING, Role.ADMIN);
        return auditLogRepo.findByOrganizationIdAndActionCodeOrderByCreatedAtDesc(
                        actor.getOrganizationId(), "EXPORT_CREATED", pageable)
                .getContent().stream()
                .map(entry -> {
                    java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", entry.getId());
                    m.put("type", entry.getObjectId());
                    m.put("status", "COMPLETED");
                    m.put("elevated", false);
                    m.put("createdAt", entry.getCreatedAt());
                    return (java.util.Map<String, Object>) m;
                })
                .toList();
    }

    private String writeExport(Long orgId, String exportType, long count) {
        try {
            Path dir = Paths.get(storageDir, "exports");
            Files.createDirectories(dir);
            Path file = dir.resolve(exportType + "-export-" + Instant.now().toEpochMilli() + ".csv");

            StringBuilder sb = new StringBuilder();
            int pageSize = 500;
            int pageNum = 0;

            switch (exportType.toLowerCase()) {
                case "ledger" -> {
                    sb.append("id,organizationId,invoiceId,refundRequestId,entryType,amount,occurredAt\n");
                    while (true) {
                        var page = ledgerRepo.findByOrganizationId(orgId, PageRequest.of(pageNum, pageSize));
                        for (var e : page.getContent()) {
                            sb.append(e.getId()).append(',')
                              .append(e.getOrganizationId()).append(',')
                              .append(e.getInvoiceId() != null ? e.getInvoiceId() : "").append(',')
                              .append(e.getRefundRequestId() != null ? e.getRefundRequestId() : "").append(',')
                              .append(e.getEntryType()).append(',')
                              .append(e.getAmount()).append(',')
                              .append(e.getOccurredAt()).append('\n');
                        }
                        if (page.isLast()) break;
                        pageNum++;
                    }
                }
                case "audit" -> {
                    sb.append("id,organizationId,actorUserId,actorUsernameSnapshot,actionCode,objectType,objectId,createdAt\n");
                    while (true) {
                        var page = auditLogRepo.findByOrganizationId(orgId, PageRequest.of(pageNum, pageSize));
                        for (var e : page.getContent()) {
                            sb.append(e.getId()).append(',')
                              .append(e.getOrganizationId()).append(',')
                              .append(e.getActorUserId() != null ? e.getActorUserId() : "").append(',')
                              .append(csvEscape(e.getActorUsernameSnapshot())).append(',')
                              .append(csvEscape(e.getActionCode())).append(',')
                              .append(csvEscape(e.getObjectType())).append(',')
                              .append(csvEscape(e.getObjectId())).append(',')
                              .append(e.getCreatedAt()).append('\n');
                        }
                        if (page.isLast()) break;
                        pageNum++;
                    }
                }
                case "patients" -> {
                    sb.append("id,organizationId,medicalRecordNumber,dateOfBirth,sex,isMinor,isProtectedCase\n");
                    while (true) {
                        var page = patientRepo.findByOrganizationId(orgId, PageRequest.of(pageNum, pageSize));
                        for (var p : page.getContent()) {
                            sb.append(p.getId()).append(',')
                              .append(p.getOrganizationId()).append(',')
                              .append(csvEscape(p.getMedicalRecordNumber())).append(',')
                              .append(p.getDateOfBirth()).append(',')
                              .append(csvEscape(p.getSex())).append(',')
                              .append(p.isMinor()).append(',')
                              .append(p.isProtectedCase()).append('\n');
                        }
                        if (page.isLast()) break;
                        pageNum++;
                    }
                }
                default -> throw new BusinessRuleException("Unknown export type: " + exportType);
            }

            Files.writeString(file, sb.toString());
            return file.toString();
        } catch (IOException e) {
            throw new BusinessRuleException("Export file write failed: " + e.getMessage());
        }
    }

    /** Wrap a value in CSV quotes if it contains a comma, quote, or newline. Null → empty string. */
    private static String csvEscape(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
