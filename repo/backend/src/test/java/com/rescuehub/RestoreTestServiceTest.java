package com.rescuehub;

import com.rescuehub.entity.BackupRun;
import com.rescuehub.entity.RestoreTestLog;
import com.rescuehub.enums.RestoreTestResult;
import com.rescuehub.exception.ForbiddenException;
import com.rescuehub.repository.BackupRunRepository;
import com.rescuehub.service.BackupService;
import com.rescuehub.service.RestoreTestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

class RestoreTestServiceTest extends BaseIntegrationTest {

    @Autowired
    private RestoreTestService restoreTestService;

    @Autowired
    private BackupService backupService;

    @Autowired
    private BackupRunRepository backupRunRepo;

    /**
     * Obtain a valid backup run. Uses runBackupAsActor which may return an
     * existing completed run (idempotency) — both cases are valid for testing.
     */
    private BackupRun getOrCreateBackupRun() {
        return backupService.runBackupAsActor(adminUser);
    }

    @Test
    @Transactional
    void record_adminRecordsRestoreTestLog_returnsSavedLog() {
        BackupRun run = getOrCreateBackupRun();

        RestoreTestLog log = restoreTestService.record(
                adminUser, run.getId(), RestoreTestResult.PASSED,
                "All tables verified successfully.",
                "127.0.0.1", "ws");

        assertNotNull(log);
        assertNotNull(log.getId());
        assertEquals(RestoreTestResult.PASSED, log.getResult());
        assertEquals(run.getId(), log.getBackupRunId());
        assertEquals(adminUser.getId(), log.getPerformedByUserId());
        assertEquals(adminUser.getOrganizationId(), log.getOrganizationId());
        assertNotNull(log.getPerformedAt());
    }

    @Test
    @Transactional
    void record_billingUserThrowsForbidden() {
        BackupRun run = getOrCreateBackupRun();

        assertThrows(ForbiddenException.class, () ->
                restoreTestService.record(
                        billingUser, run.getId(), RestoreTestResult.PASSED,
                        "Should not succeed",
                        "127.0.0.1", "ws"));
    }

    @Test
    @Transactional
    void list_adminCanListRestoreTestLogs() {
        BackupRun run = getOrCreateBackupRun();
        restoreTestService.record(
                adminUser, run.getId(), RestoreTestResult.PASSED,
                "Partial restore verified",
                "127.0.0.1", "ws");

        Page<RestoreTestLog> page = restoreTestService.list(
                adminUser, PageRequest.of(0, 20));

        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 1);
        page.getContent().forEach(log ->
                assertEquals(adminUser.getOrganizationId(), log.getOrganizationId()));
    }
}
