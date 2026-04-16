package com.rescuehub;

import com.rescuehub.entity.LedgerEntry;
import com.rescuehub.entity.Patient;
import com.rescuehub.enums.LedgerEntryType;
import com.rescuehub.exception.BusinessRuleException;
import com.rescuehub.exception.ForbiddenException;
import com.rescuehub.repository.LedgerEntryRepository;
import com.rescuehub.repository.PatientRepository;
import com.rescuehub.service.ExportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ExportServiceTest extends BaseIntegrationTest {

    @Autowired private ExportService exportService;
    @Autowired private PatientRepository patientRepo;
    @Autowired private LedgerEntryRepository ledgerRepo;

    @Test
    void exportLedgerSucceeds() {
        String key = "export-ledger-" + UUID.randomUUID();
        String result = exportService.export(billingUser, "ledger", key, true, true, "127.0.0.1", "ws1");
        assertNotNull(result);
        assertTrue(result.contains("ledger"));
    }

    @Test
    void exportRequiresBillingRole() {
        String key = "export-forbidden-" + UUID.randomUUID();
        assertThrows(ForbiddenException.class, () ->
                exportService.export(clinicianUser, "ledger", key, true, true, "127.0.0.1", "ws1"));
    }

    @Test
    void smallExportNoElevationRequired() {
        String key = "export-small-" + UUID.randomUUID();
        assertDoesNotThrow(() ->
                exportService.export(billingUser, "ledger", key, false, false, "127.0.0.1", "ws1"));
    }

    @Test
    void idempotentExport() {
        String key = "export-idem-" + UUID.randomUUID();
        String r1 = exportService.export(adminUser, "ledger", key, true, true, "127.0.0.1", "ws1");
        String r2 = exportService.export(adminUser, "ledger", key, true, true, "127.0.0.1", "ws1");
        assertEquals(r1, r2, "Second export with same key should return cached result");
    }

    @Test
    void listHistory_adminGetsExportHistory() {
        String key = "history-" + UUID.randomUUID();
        exportService.export(adminUser, "ledger", key, true, true, "127.0.0.1", "ws1");

        List<Map<String, Object>> history =
                exportService.listHistory(adminUser, PageRequest.of(0, 20));
        assertNotNull(history);
        assertTrue(history.size() >= 1);
        assertEquals("COMPLETED", history.get(0).get("status"));
    }

    @Test
    void listHistory_clinicianForbidden() {
        assertThrows(ForbiddenException.class,
                () -> exportService.listHistory(clinicianUser, PageRequest.of(0, 20)));
    }

    @Test
    void exportAudit_succeeds() {
        String key = "export-audit-" + UUID.randomUUID();
        String result = exportService.export(adminUser, "audit", key, true, true, "127.0.0.1", "ws1");
        assertNotNull(result);
        assertTrue(result.contains("audit"));
    }

    @Test
    void exportPatients_succeeds() {
        String key = "export-patients-" + UUID.randomUUID();
        String result = exportService.export(adminUser, "patients", key, true, true, "127.0.0.1", "ws1");
        assertNotNull(result);
        assertTrue(result.contains("patients"));
    }

    @Test
    void exportUnknownType_throwsBusinessRule() {
        String key = "export-unknown-" + UUID.randomUUID();
        assertThrows(BusinessRuleException.class,
                () -> exportService.export(adminUser, "unknown_type", key, true, true, "127.0.0.1", "ws1"));
    }

    /** Ensures the ledger CSV for-loop body executes by seeding a ledger entry first. */
    @Test
    void exportLedger_withData_coversRowIteration() {
        LedgerEntry entry = new LedgerEntry();
        entry.setOrganizationId(testOrg.getId());
        entry.setEntryType(LedgerEntryType.INVOICE_GENERATED);
        entry.setAmount(new BigDecimal("75.00"));
        entry.setOccurredAt(Instant.now());
        ledgerRepo.save(entry);

        String key = "export-ledger-rows-" + UUID.randomUUID();
        String result = exportService.export(adminUser, "ledger", key, true, true, "127.0.0.1", "ws1");
        assertNotNull(result);
        assertTrue(result.contains("ledger"));
    }

    /** Ensures the audit CSV for-loop body executes: seed an audit log via a prior export, then export audit. */
    @Test
    void exportAudit_withData_coversRowIteration() {
        // A committed export call creates an EXPORT_CREATED audit log
        exportService.export(adminUser, "ledger", "seed-audit-" + UUID.randomUUID(), true, true, "127.0.0.1", "ws1");

        String key = "export-audit-rows-" + UUID.randomUUID();
        String result = exportService.export(adminUser, "audit", key, true, true, "127.0.0.1", "ws1");
        assertNotNull(result);
        assertTrue(result.contains("audit"));
    }

    /** Ensures the patients CSV for-loop body executes by seeding a patient first. */
    @Test
    void exportPatients_withData_coversRowIteration() {
        Patient p = new Patient();
        p.setOrganizationId(testOrg.getId());
        p.setMedicalRecordNumber("EXP-PAT-" + System.nanoTime());
        p.setDateOfBirth(LocalDate.of(1990, 1, 1));
        patientRepo.save(p);

        String key = "export-patients-rows-" + UUID.randomUUID();
        String result = exportService.export(adminUser, "patients", key, true, true, "127.0.0.1", "ws1");
        assertNotNull(result);
        assertTrue(result.contains("patients"));
    }
}
