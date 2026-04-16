package com.rescuehub;

import com.rescuehub.entity.BackupRun;
import com.rescuehub.enums.BackupStatus;
import com.rescuehub.exception.ForbiddenException;
import com.rescuehub.repository.BackupRunRepository;
import com.rescuehub.service.BackupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BackupServiceTest extends BaseIntegrationTest {

    @Autowired
    private BackupService backupService;

    @Autowired
    private BackupRunRepository backupRunRepo;

    @Test
    @Transactional
    void runBackupAsActor_adminSucceeds() {
        BackupRun run = backupService.runBackupAsActor(adminUser);

        assertNotNull(run);
        assertEquals(BackupStatus.COMPLETED, run.getStatus());
        assertNotNull(run.getOutputPath());
        assertEquals(adminUser.getOrganizationId(), run.getOrganizationId());
    }

    @Test
    @Transactional
    void runBackupAsActor_nonAdminThrowsForbidden() {
        assertThrows(ForbiddenException.class,
                () -> backupService.runBackupAsActor(billingUser));
    }

    @Test
    void runBackup_idempotencyWithin23Hours_returnsSameRun() {
        // First call
        BackupRun first = backupService.runBackupAsActor(adminUser);
        assertNotNull(first);
        assertEquals(BackupStatus.COMPLETED, first.getStatus());

        // Second call within 23-hour window — must return same run
        BackupRun second = backupService.runBackupAsActor(adminUser);
        assertEquals(first.getId(), second.getId(),
                "Second backup within 23 hours must return the same run (idempotency)");
    }

    @Test
    @Transactional
    void list_adminCanListBackupsForOrg() {
        backupService.runBackupAsActor(adminUser);

        Page<BackupRun> page = backupService.list(adminUser, PageRequest.of(0, 10));
        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 1);
        page.getContent().forEach(run ->
                assertEquals(adminUser.getOrganizationId(), run.getOrganizationId()));
    }

    @Test
    @Transactional
    void list_nonAdminThrowsForbidden() {
        assertThrows(ForbiddenException.class,
                () -> backupService.list(billingUser, PageRequest.of(0, 10)));
    }

    @Test
    void cleanupExpired_deletesExpiredRuns() {
        // Create a backup run and manually expire it
        BackupRun run = backupService.runBackupAsActor(adminUser);
        Long runId = run.getId();

        // Set retentionExpiresAt to the past
        run.setRetentionExpiresAt(Instant.now().minusSeconds(3600));
        backupRunRepo.save(run);

        // Verify it exists before cleanup
        assertTrue(backupRunRepo.findById(runId).isPresent());

        // Run cleanup
        backupService.cleanupExpired(adminUser.getOrganizationId());

        // Verify it was deleted
        assertFalse(backupRunRepo.findById(runId).isPresent(),
                "Expired backup run must be deleted after cleanupExpired");
    }
}
