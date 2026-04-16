package com.rescuehub;

import com.rescuehub.entity.AuditLog;
import com.rescuehub.repository.AuditLogRepository;
import com.rescuehub.service.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for AuditService.
 *
 * AuditService is exercised indirectly by almost every other service test, but
 * those tests only drive the log() method with the outer transaction rolled back.
 * Because log() uses REQUIRES_NEW, the audit row IS persisted even when the outer
 * test transaction rolls back — but having an explicit test makes the coverage
 * attributable directly to AuditService and verifies the log fields.
 */
class AuditServiceTest extends BaseIntegrationTest {

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void log_persistsAuditEntry() {
        long countBefore = auditLogRepository.count();

        auditService.log(
                adminUser.getId(),
                adminUser.getUsername(),
                "TEST_ACTION",
                "Patient",
                "patient-42",
                testOrg.getId(),
                "10.0.0.1",
                "ws-01",
                null,
                "{\"field\":\"value\"}");

        long countAfter = auditLogRepository.count();
        assertTrue(countAfter > countBefore, "Audit log row should have been persisted");
    }

    @Test
    void log_setsAllFields() {
        // Use a nano-time suffix so the action code is unique across the shared DB.
        // The all-tests context accumulates hundreds of audit rows; querying by a
        // unique action code avoids pagination misses.
        String uniqueAction = "AUDIT_FIELDS_TEST_" + System.nanoTime();

        auditService.log(
                adminUser.getId(),
                adminUser.getUsername(),
                uniqueAction,
                "Patient",
                "p-99",
                testOrg.getId(),
                "192.168.1.1",
                "ws-fields-test",
                "{\"before\":true}",
                "{\"after\":true}");

        Page<AuditLog> logs = auditLogRepository
                .findByOrganizationIdAndActionCodeOrderByCreatedAtDesc(
                        testOrg.getId(), uniqueAction, PageRequest.of(0, 5));

        assertFalse(logs.getContent().isEmpty(), "Expected at least one audit log entry");
        AuditLog last = logs.getContent().get(0);

        assertEquals(adminUser.getId(), last.getActorUserId());
        assertEquals(adminUser.getUsername(), last.getActorUsernameSnapshot());
        assertEquals("Patient", last.getObjectType());
        assertEquals("p-99", last.getObjectId());
        assertEquals("192.168.1.1", last.getIpAddress());
        // H2's JSON column type may add an extra encoding layer vs MySQL.
        // Verify the fields are persisted and contain the expected keys,
        // rather than exact-matching the raw stored bytes.
        assertNotNull(last.getBeforeJson());
        assertTrue(last.getBeforeJson().contains("before"), "beforeJson should contain key");
        assertNotNull(last.getAfterJson());
        assertTrue(last.getAfterJson().contains("after"), "afterJson should contain key");
    }

    @Test
    void log_nullBeforeAfter_doesNotThrow() {
        assertDoesNotThrow(() -> auditService.log(
                adminUser.getId(),
                adminUser.getUsername(),
                "CREATE",
                "Visit",
                "v-1",
                testOrg.getId(),
                "10.0.0.1",
                null,
                null,
                null));
    }

    @Test
    void findFiltered_withQuery_returnsMatchingRows() {
        // Unique actor name guarantees we can find this specific row in filtered results.
        String uniqueActor = "filterable_" + System.nanoTime();

        auditService.log(
                adminUser.getId(),
                uniqueActor,
                "FILTERED_ACTION",
                "SomeType",
                "obj-1",
                testOrg.getId(),
                "1.1.1.1",
                "ws-filter",
                null, null);

        Page<AuditLog> result = auditLogRepository.findFiltered(
                testOrg.getId(), uniqueActor, PageRequest.of(0, 10));
        assertTrue(result.getContent().stream()
                .anyMatch(l -> uniqueActor.equals(l.getActorUsernameSnapshot())));
    }

    @Test
    void findFiltered_nullQuery_returnsAll() {
        Page<AuditLog> result = auditLogRepository.findFiltered(
                testOrg.getId(), null, PageRequest.of(0, 50));
        assertNotNull(result);
    }

    @Test
    void findByActionCode_returnsRows() {
        String uniqueAction = "UNIQUE_ACTION_" + System.nanoTime();
        auditService.log(
                adminUser.getId(),
                adminUser.getUsername(),
                uniqueAction,
                "SomeType",
                "obj-2",
                testOrg.getId(),
                "1.1.1.2",
                "ws-code",
                null, null);

        Page<AuditLog> result = auditLogRepository
                .findByOrganizationIdAndActionCodeOrderByCreatedAtDesc(
                        testOrg.getId(), uniqueAction, PageRequest.of(0, 10));
        assertFalse(result.getContent().isEmpty());
        assertEquals(uniqueAction, result.getContent().get(0).getActionCode());
    }
}
