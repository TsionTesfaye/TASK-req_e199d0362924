package com.rescuehub.service;

import com.rescuehub.entity.BackupRun;
import com.rescuehub.entity.User;
import com.rescuehub.enums.BackupStatus;
import com.rescuehub.repository.BackupRunRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BackupService {

    @Value("${rescuehub.storage.dir}")
    private String storageDir;

    private final BackupRunRepository backupRunRepo;
    private final AuditService auditService;
    private final JdbcTemplate jdbc;

    public BackupService(BackupRunRepository backupRunRepo, AuditService auditService, JdbcTemplate jdbc) {
        this.backupRunRepo = backupRunRepo;
        this.auditService = auditService;
        this.jdbc = jdbc;
    }

    /**
     * Operator-initiated backup. Service-layer ADMIN check (callers from controllers
     * and from authenticated admin contexts only). Scheduler uses the SYSTEM overload below.
     */
    @Transactional
    public BackupRun runBackupAsActor(com.rescuehub.entity.User actor) {
        if (actor == null) throw new com.rescuehub.exception.ForbiddenException("Authentication required");
        if (actor.getRole() != com.rescuehub.enums.Role.ADMIN) {
            throw new com.rescuehub.exception.ForbiddenException("Only ADMIN may run backups");
        }
        return runBackup(actor.getOrganizationId(), actor.getUsername(), actor.getId());
    }

    @Transactional
    public BackupRun runBackup(Long orgId, String actorUsername, Long actorId) {
        // Idempotency: at most one COMPLETED backup per org per 23-hour window to avoid
        // duplicate nightly-job execution producing two artifacts.
        Instant cutoff = Instant.now().minusSeconds(23L * 3600);
        List<BackupRun> recent = backupRunRepo.findByOrganizationIdAndCreatedAtAfter(orgId, cutoff);
        for (BackupRun r : recent) {
            if (r.getStatus() == BackupStatus.COMPLETED) {
                return r; // no-op, existing backup still current
            }
        }
        BackupRun run = new BackupRun();
        run.setOrganizationId(orgId);
        run.setBackupType("NIGHTLY");
        run.setStatus(BackupStatus.RUNNING);
        run.setRetentionExpiresAt(Instant.now().plusSeconds(30L * 24 * 3600));
        run = backupRunRepo.save(run);

        try {
            String filePath = writeBackupFile(run.getId(), orgId);
            run.setOutputPath(filePath);
            run.setStatus(BackupStatus.COMPLETED);
            run.setCompletedAt(Instant.now());
            run = backupRunRepo.save(run);

            auditService.log(actorId, actorUsername, "BACKUP_COMPLETED",
                    "BackupRun", String.valueOf(run.getId()), orgId, null, null, null,
                    "{\"path\":\"" + filePath + "\"}");
        } catch (Exception e) {
            run.setStatus(BackupStatus.FAILED);
            run.setCompletedAt(Instant.now());
            run = backupRunRepo.save(run);
            auditService.log(actorId, actorUsername, "BACKUP_FAILED",
                    "BackupRun", String.valueOf(run.getId()), orgId, null, null, null,
                    "{\"error\":\"" + e.getMessage() + "\"}");
        }
        return run;
    }

    /** Tables dumped per-org. Order matters for FK: patients before visits, etc. */
    private static final String[] DUMP_TABLES = {
        "patient", "visit", "visit_charge", "invoice", "invoice_tender",
        "payment", "refund_request", "ledger_entry",
        "daily_close", "corrective_action", "quality_rule_result",
        "incident_report", "audit_log"
    };

    private String writeBackupFile(Long runId, Long orgId) throws IOException {
        Path dir = Paths.get(storageDir, "backups");
        Files.createDirectories(dir);
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        Path file = dir.resolve("backup-" + timestamp + ".sql");

        StringBuilder sb = new StringBuilder();
        sb.append("-- RescueHub backup run ").append(runId)
          .append(", org ").append(orgId)
          .append(", generated ").append(Instant.now()).append("\n");
        sb.append("SET FOREIGN_KEY_CHECKS=0;\n\n");

        for (String table : DUMP_TABLES) {
            appendTableDump(sb, table, orgId);
        }

        sb.append("SET FOREIGN_KEY_CHECKS=1;\n");
        Files.writeString(file, sb.toString());
        return file.toString();
    }

    private void appendTableDump(StringBuilder sb, String table, Long orgId) {
        List<Map<String, Object>> rows;
        try {
            rows = jdbc.queryForList(
                "SELECT * FROM `" + table + "` WHERE organization_id = ?", orgId);
        } catch (Exception e) {
            sb.append("-- SKIP ").append(table).append(": ").append(e.getMessage()).append("\n\n");
            return;
        }
        sb.append("-- ").append(table).append(": ").append(rows.size()).append(" rows\n");
        for (Map<String, Object> row : rows) {
            sb.append(buildInsert(table, row)).append("\n");
        }
        sb.append("\n");
    }

    private static String buildInsert(String table, Map<String, Object> row) {
        List<String> cols = new ArrayList<>(row.keySet());
        String colList = cols.stream().map(c -> "`" + c + "`").collect(Collectors.joining(", "));
        String valList = cols.stream().map(c -> sqlLiteral(row.get(c))).collect(Collectors.joining(", "));
        return "INSERT INTO `" + table + "` (" + colList + ") VALUES (" + valList + ");";
    }

    private static String sqlLiteral(Object val) {
        if (val == null) return "NULL";
        if (val instanceof byte[]) {
            return "X'" + HexFormat.of().formatHex((byte[]) val) + "'";
        }
        if (val instanceof Boolean) return ((Boolean) val) ? "1" : "0";
        if (val instanceof Number) return val.toString();
        // Timestamps, dates, and strings: quote and escape
        String s = val.toString();
        return "'" + s.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    @Transactional
    public void cleanupExpired(Long orgId) {
        List<BackupRun> expired = backupRunRepo.findByOrganizationIdAndRetentionExpiresAtBefore(orgId, Instant.now());
        for (BackupRun run : expired) {
            if (run.getOutputPath() != null) {
                try { Paths.get(run.getOutputPath()).toFile().delete(); } catch (Exception ignore) {}
            }
            backupRunRepo.delete(run);
        }
    }

    @Transactional(readOnly = true)
    public Page<BackupRun> list(User actor, Pageable pageable) {
        if (actor == null) throw new com.rescuehub.exception.ForbiddenException("Authentication required");
        if (actor.getRole() != com.rescuehub.enums.Role.ADMIN) {
            throw new com.rescuehub.exception.ForbiddenException("Only ADMIN may list backups");
        }
        return backupRunRepo.findByOrganizationId(actor.getOrganizationId(), pageable);
    }
}
