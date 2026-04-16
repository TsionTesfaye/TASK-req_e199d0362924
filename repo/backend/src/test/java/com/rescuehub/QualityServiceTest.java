package com.rescuehub;

import com.rescuehub.entity.*;
import com.rescuehub.enums.*;
import com.rescuehub.exception.BusinessRuleException;
import com.rescuehub.repository.*;
import com.rescuehub.service.QualityRulesService;
import com.rescuehub.service.VisitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class QualityServiceTest extends BaseIntegrationTest {

    @Autowired private QualityRulesService qualityService;
    @Autowired private VisitService visitService;
    @Autowired private VisitRepository visitRepo;
    @Autowired private PatientRepository patientRepo;
    @Autowired private QualityRuleResultRepository resultRepo;
    @Autowired private IdempotencyKeyRepository idempotencyRepo;

    @Test
    @Transactional
    void blockingRuleBlocksVisitClose() {
        Patient patient = new Patient();
        patient.setOrganizationId(testOrg.getId());
        patient.setMedicalRecordNumber("QC-TEST-" + System.currentTimeMillis());
        patient.setDateOfBirth(LocalDate.of(1990, 1, 1));
        patient = patientRepo.save(patient);

        Visit visit = new Visit();
        visit.setOrganizationId(testOrg.getId());
        visit.setPatientId(patient.getId());
        visit.setCreatedByUserId(clinicianUser.getId());
        visit.setOpenedAt(Instant.now());
        visit.setStatus(VisitStatus.OPEN);
        visit = visitRepo.save(visit);

        // Manually insert a blocking rule result
        QualityRuleResult result = new QualityRuleResult();
        result.setOrganizationId(testOrg.getId());
        result.setVisitId(visit.getId());
        result.setRuleCode("TEST_BLOCK");
        result.setSeverity(QualitySeverity.BLOCKING);
        result.setStatus(QualityResultStatus.OPEN);
        resultRepo.save(result);

        // Attempt to close visit — should fail
        String idempKey = "close-key-" + System.currentTimeMillis();
        Long visitId = visit.getId();
        assertThrows(BusinessRuleException.class, () ->
                visitService.closeVisit(clinicianUser, visitId, idempKey, "127.0.0.1", "ws1"));
    }

    @Test
    @Transactional
    void getResult_returnsResultById() {
        Patient patient = new Patient();
        patient.setOrganizationId(testOrg.getId());
        patient.setMedicalRecordNumber("QC-GET-" + System.nanoTime());
        patient.setDateOfBirth(LocalDate.of(1990, 1, 1));
        patient = patientRepo.save(patient);

        Visit visit = new Visit();
        visit.setOrganizationId(testOrg.getId());
        visit.setPatientId(patient.getId());
        visit.setCreatedByUserId(clinicianUser.getId());
        visit.setOpenedAt(Instant.now());
        visit.setStatus(VisitStatus.OPEN);
        visit = visitRepo.save(visit);

        QualityRuleResult result = new QualityRuleResult();
        result.setOrganizationId(testOrg.getId());
        result.setVisitId(visit.getId());
        result.setRuleCode("GET_RESULT_TEST");
        result.setSeverity(QualitySeverity.WARNING);
        result.setStatus(QualityResultStatus.OPEN);
        result = resultRepo.save(result);

        QualityRuleResult fetched = qualityService.getResult(qualityUser, result.getId());
        assertNotNull(fetched);
        assertEquals(result.getId(), fetched.getId());
        assertEquals("GET_RESULT_TEST", fetched.getRuleCode());
    }

    @Test
    @Transactional
    void getResult_notFound_throwsNotFoundException() {
        assertThrows(com.rescuehub.exception.NotFoundException.class, () ->
                qualityService.getResult(qualityUser, Long.MAX_VALUE));
    }

    @Test
    @Transactional
    void getResult_clinicianForbidden() {
        assertThrows(com.rescuehub.exception.ForbiddenException.class, () ->
                qualityService.getResult(clinicianUser, 1L));
    }

    @Test
    @Transactional
    void overrideUnblocksVisit() {
        Patient patient = new Patient();
        patient.setOrganizationId(testOrg.getId());
        patient.setMedicalRecordNumber("QC-OVERRIDE-" + System.currentTimeMillis());
        patient.setDateOfBirth(LocalDate.of(1990, 1, 1));
        patient = patientRepo.save(patient);

        Visit visit = new Visit();
        visit.setOrganizationId(testOrg.getId());
        visit.setPatientId(patient.getId());
        visit.setCreatedByUserId(clinicianUser.getId());
        visit.setOpenedAt(Instant.now());
        visit.setStatus(VisitStatus.OPEN);
        visit.setChiefComplaint("Test complaint");
        visit = visitRepo.save(visit);

        // Manually insert a blocking rule result
        QualityRuleResult result = new QualityRuleResult();
        result.setOrganizationId(testOrg.getId());
        result.setVisitId(visit.getId());
        result.setRuleCode("TEST_BLOCK_2");
        result.setSeverity(QualitySeverity.BLOCKING);
        result.setStatus(QualityResultStatus.OPEN);
        result = resultRepo.save(result);

        // Override by quality user
        qualityService.override(qualityUser, result.getId(), "CLINICAL_JUSTIFICATION",
                "Override note here", "127.0.0.1", "ws1");

        // Now visit should close successfully
        String idempKey = "close-after-override-" + System.currentTimeMillis();
        Long visitId = visit.getId();
        assertDoesNotThrow(() -> visitService.closeVisit(clinicianUser, visitId, idempKey, "127.0.0.1", "ws1"));
    }

    @Test
    @Transactional
    void runRules_missingChiefComplaint_createsWarningResult() {
        Patient patient = new Patient();
        patient.setOrganizationId(testOrg.getId());
        patient.setMedicalRecordNumber("QC-MCC-" + System.nanoTime());
        patient.setDateOfBirth(LocalDate.of(1990, 1, 1)); // adult, no age mismatch
        patient.setMinor(false);
        patient = patientRepo.save(patient);

        // Open visit with null chief complaint — triggers MISSING_CHIEF_COMPLAINT rule
        Visit visit = visitService.open(clinicianUser, patient.getId(), null,
                null, null, "127.0.0.1", "ws1");

        var results = resultRepo.findByVisitIdAndSeverityAndStatus(
                visit.getId(), QualitySeverity.WARNING, QualityResultStatus.OPEN);
        assertTrue(results.stream().anyMatch(r -> "MISSING_CHIEF_COMPLAINT".equals(r.getRuleCode())),
                "Missing chief complaint should create a WARNING QC result");
    }

    @Test
    @Transactional
    void runRules_ageDobMismatch_createsWarningResult() {
        Patient patient = new Patient();
        patient.setOrganizationId(testOrg.getId());
        patient.setMedicalRecordNumber("QC-DOB-" + System.nanoTime());
        // Patient is marked as NOT a minor, but DOB says they are (born 10 years ago)
        patient.setDateOfBirth(LocalDate.now().minusYears(10));
        patient.setMinor(false);
        patient = patientRepo.save(patient);

        Visit visit = visitService.open(clinicianUser, patient.getId(), null,
                "Complaint", null, "127.0.0.1", "ws1");

        var results = resultRepo.findByVisitIdAndSeverityAndStatus(
                visit.getId(), QualitySeverity.WARNING, QualityResultStatus.OPEN);
        assertTrue(results.stream().anyMatch(r -> "AGE_DOB_MISMATCH".equals(r.getRuleCode())),
                "Minor-flag vs DOB mismatch should create a WARNING QC result");
    }

    @Test
    @Transactional
    void runRules_highVisitFrequency_setsQcBlocked() {
        Patient patient = new Patient();
        patient.setOrganizationId(testOrg.getId());
        patient.setMedicalRecordNumber("QC-HVF-" + System.nanoTime());
        patient.setDateOfBirth(LocalDate.of(1990, 1, 1));
        patient.setMinor(false);
        patient = patientRepo.save(patient);

        // Directly create 3 closed visits within the 7-day window
        for (int i = 0; i < 3; i++) {
            Visit v = new Visit();
            v.setOrganizationId(testOrg.getId());
            v.setPatientId(patient.getId());
            v.setCreatedByUserId(clinicianUser.getId());
            v.setOpenedAt(Instant.now().minusSeconds(3600));
            v.setClosedAt(Instant.now().minusSeconds(1800));
            v.setStatus(VisitStatus.CLOSED);
            v.setChiefComplaint("Past complaint " + i);
            visitRepo.save(v);
        }

        // Open a 4th visit — HIGH_VISIT_FREQUENCY rule should fire (BLOCKING) and set qcBlocked
        Visit visit = visitService.open(clinicianUser, patient.getId(), null,
                "New complaint", null, "127.0.0.1", "ws1");

        assertTrue(visit.isQcBlocked(), "Visit should be marked qcBlocked due to high frequency rule");
        var results = resultRepo.findByVisitIdAndSeverityAndStatus(
                visit.getId(), QualitySeverity.BLOCKING, QualityResultStatus.OPEN);
        assertTrue(results.stream().anyMatch(r -> "HIGH_VISIT_FREQUENCY".equals(r.getRuleCode())),
                "High visit frequency should create a BLOCKING QC result");
    }

    @Test
    @Transactional
    void override_blankNote_throwsBusinessRule() {
        QualityRuleResult result = new QualityRuleResult();
        result.setOrganizationId(testOrg.getId());
        result.setRuleCode("TEST_BLANK_NOTE");
        result.setSeverity(QualitySeverity.WARNING);
        result.setStatus(QualityResultStatus.OPEN);
        result = resultRepo.save(result);
        final Long resultId = result.getId();
        assertThrows(BusinessRuleException.class, () ->
                qualityService.override(qualityUser, resultId, "REASON", "",
                        "127.0.0.1", "ws1"));
    }
}
