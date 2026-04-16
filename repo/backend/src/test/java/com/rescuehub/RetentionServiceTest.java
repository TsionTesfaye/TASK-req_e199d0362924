package com.rescuehub;

import com.rescuehub.entity.Patient;
import com.rescuehub.entity.RetentionPolicyHold;
import com.rescuehub.repository.PatientRepository;
import com.rescuehub.repository.RetentionPolicyHoldRepository;
import com.rescuehub.service.RetentionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class RetentionServiceTest extends BaseIntegrationTest {

    @Autowired
    private RetentionService retentionService;

    @Autowired
    private PatientRepository patientRepo;

    @Autowired
    private RetentionPolicyHoldRepository holdRepo;

    @Autowired
    private JdbcTemplate jdbc;

    private Patient createPatient(String suffix) {
        Patient p = new Patient();
        p.setOrganizationId(testOrg.getId());
        p.setMedicalRecordNumber("RET-" + suffix);
        p.setDateOfBirth(LocalDate.of(1980, 1, 1));
        p.setSex("F");
        return patientRepo.save(p);
    }

    @Test
    void archiveEligible_archivesOldPatientButNotNewOne() {
        long nanos = System.nanoTime();

        // Patient created 8 years ago (beyond 7-year threshold)
        Patient oldPatient = createPatient("old-" + nanos);
        Instant eightYearsAgo = Instant.now().minus(8 * 365, ChronoUnit.DAYS);
        jdbc.update("UPDATE patient SET updated_at = ? WHERE id = ?",
                eightYearsAgo, oldPatient.getId());

        // Patient created 1 year ago (within threshold)
        Patient newPatient = createPatient("new-" + nanos);
        Instant oneYearAgo = Instant.now().minus(365, ChronoUnit.DAYS);
        jdbc.update("UPDATE patient SET updated_at = ? WHERE id = ?",
                oneYearAgo, newPatient.getId());

        int archived = retentionService.archiveEligible(testOrg.getId());

        assertTrue(archived >= 1, "At least one patient should be archived");

        // Reload patients from DB to check archivedAt
        Patient reloadedOld = patientRepo.findById(oldPatient.getId()).orElseThrow();
        Patient reloadedNew = patientRepo.findById(newPatient.getId()).orElseThrow();

        assertNotNull(reloadedOld.getArchivedAt(), "Old patient must be archived");
        assertNull(reloadedNew.getArchivedAt(), "New patient must NOT be archived");
    }

    @Test
    void archiveEligible_withLegalHold_patientNotArchived() {
        long nanos = System.nanoTime();

        // Create an old patient
        Patient oldPatient = createPatient("hold-" + nanos);
        Instant eightYearsAgo = Instant.now().minus(8 * 365, ChronoUnit.DAYS);
        jdbc.update("UPDATE patient SET updated_at = ? WHERE id = ?",
                eightYearsAgo, oldPatient.getId());

        // Create a retention policy hold with a future holdUntil
        RetentionPolicyHold hold = new RetentionPolicyHold();
        hold.setPatientId(oldPatient.getId());
        hold.setHoldReason("Legal hold for test");
        hold.setHoldUntil(Instant.now().plus(365, ChronoUnit.DAYS));
        hold.setCreatedByUserId(adminUser.getId());
        holdRepo.save(hold);

        retentionService.archiveEligible(testOrg.getId());

        // Reload and verify NOT archived due to hold
        Patient reloaded = patientRepo.findById(oldPatient.getId()).orElseThrow();
        assertNull(reloaded.getArchivedAt(),
                "Patient on legal hold must NOT be archived");
    }
}
