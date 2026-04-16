package com.rescuehub;

import com.rescuehub.entity.Patient;
import com.rescuehub.entity.Visit;
import com.rescuehub.enums.VisitStatus;
import com.rescuehub.exception.BusinessRuleException;
import com.rescuehub.exception.ForbiddenException;
import com.rescuehub.exception.NotFoundException;
import com.rescuehub.repository.PatientRepository;
import com.rescuehub.repository.VisitRepository;
import com.rescuehub.service.VisitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class VisitServiceTest extends BaseIntegrationTest {

    @Autowired
    private VisitService visitService;

    @Autowired
    private PatientRepository patientRepo;

    @Autowired
    private VisitRepository visitRepo;

    private Patient createPatient(String suffix) {
        Patient p = new Patient();
        p.setOrganizationId(testOrg.getId());
        p.setMedicalRecordNumber("VS-" + suffix);
        p.setDateOfBirth(LocalDate.of(1985, 6, 15));
        p.setSex("M");
        return patientRepo.save(p);
    }

    @Test
    @Transactional
    void updateSummary_updatesSummaryAndDiagnosis() {
        Patient patient = createPatient("upd-" + System.nanoTime());
        Visit visit = visitService.open(clinicianUser, patient.getId(), null,
                "Chief complaint", null, "127.0.0.1", "ws1");

        Visit updated = visitService.updateSummary(clinicianUser, visit.getId(),
                "Summary text", "Diagnosis text", "127.0.0.1", "ws1");

        assertEquals("Summary text", updated.getSummaryText());
        assertEquals("Diagnosis text", updated.getDiagnosisText());
    }

    @Test
    @Transactional
    void updateSummary_billingForbidden() {
        Patient patient = createPatient("upd-fbd-" + System.nanoTime());
        Visit visit = visitService.open(clinicianUser, patient.getId(), null,
                "Chief complaint", null, "127.0.0.1", "ws1");

        assertThrows(ForbiddenException.class,
                () -> visitService.updateSummary(billingUser, visit.getId(),
                        "Summary", "Diagnosis", "127.0.0.1", "ws1"));
    }

    @Test
    @Transactional
    void updateSummary_closedVisit_throwsBusinessRule() {
        Patient patient = createPatient("upd-closed-" + System.nanoTime());
        Visit visit = visitService.open(clinicianUser, patient.getId(), null,
                "Complaint", null, "127.0.0.1", "ws1");
        visitService.closeVisit(clinicianUser, visit.getId(),
                "close-key-" + System.nanoTime(), "127.0.0.1", "ws1");

        assertThrows(BusinessRuleException.class,
                () -> visitService.updateSummary(clinicianUser, visit.getId(),
                        "Summary", "Diagnosis", "127.0.0.1", "ws1"));
    }

    @Test
    @Transactional
    void getById_returnsVisit() {
        Patient patient = createPatient("get-" + System.nanoTime());
        Visit visit = visitService.open(clinicianUser, patient.getId(), null,
                "Headache", null, "127.0.0.1", "ws1");

        Visit fetched = visitService.getById(clinicianUser, visit.getId());
        assertEquals(visit.getId(), fetched.getId());
        assertEquals(VisitStatus.OPEN, fetched.getStatus());
    }

    @Test
    @Transactional
    void getById_notFound_throwsNotFoundException() {
        assertThrows(NotFoundException.class,
                () -> visitService.getById(clinicianUser, Long.MAX_VALUE));
    }

    @Test
    @Transactional
    void list_returnsVisitsForOrg() {
        Patient patient = createPatient("list-" + System.nanoTime());
        visitService.open(clinicianUser, patient.getId(), null,
                "Chest pain", null, "127.0.0.1", "ws1");

        Page<Visit> page = visitService.list(clinicianUser, null, PageRequest.of(0, 20));
        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 1);
    }

    @Test
    @Transactional
    void list_filteredByPatientId_returnsMatchingVisits() {
        Patient patient = createPatient("list-pid-" + System.nanoTime());
        Visit v = visitService.open(clinicianUser, patient.getId(), null,
                "Pain", null, "127.0.0.1", "ws1");

        Page<Visit> page = visitService.list(clinicianUser, patient.getId(), PageRequest.of(0, 20));
        assertNotNull(page);
        assertTrue(page.getContent().stream().anyMatch(visit -> visit.getId().equals(v.getId())));
    }

    @Test
    @Transactional
    void list_billingForbidden() {
        assertThrows(ForbiddenException.class,
                () -> visitService.list(billingUser, null, PageRequest.of(0, 20)));
    }

    @Test
    @Transactional
    void open_idempotencyReplay_returnsSameVisit() {
        Patient patient = createPatient("idem-open-" + System.nanoTime());
        String key = "visit-open-idem-" + System.nanoTime();

        Visit first = visitService.open(clinicianUser, patient.getId(), null,
                "Headache", key, "127.0.0.1", "ws1");
        Visit second = visitService.open(clinicianUser, patient.getId(), null,
                "Headache", key, "127.0.0.1", "ws1");

        assertEquals(first.getId(), second.getId(),
                "Idempotency replay must return the same visit ID");
    }

    @Test
    @Transactional
    void closeVisit_alreadyClosedStatus_withDifferentKey_returnsVisit() {
        Patient patient = createPatient("close-dup-" + System.nanoTime());
        Visit visit = visitService.open(clinicianUser, patient.getId(), null,
                "Complaint", null, "127.0.0.1", "ws1");
        // First close
        visitService.closeVisit(clinicianUser, visit.getId(),
                "close-key-first-" + System.nanoTime(), "127.0.0.1", "ws1");
        // Second close with a different key — hits the CLOSED-status branch
        Visit result = visitService.closeVisit(clinicianUser, visit.getId(),
                "close-key-second-" + System.nanoTime(), "127.0.0.1", "ws1");
        assertNotNull(result);
        assertEquals(VisitStatus.CLOSED, result.getStatus());
    }

    @Test
    @Transactional
    void closeVisit_archivedVisit_throwsBusinessRule() {
        Patient patient = createPatient("close-arch-" + System.nanoTime());
        Visit visit = visitService.open(clinicianUser, patient.getId(), null,
                "Complaint", null, "127.0.0.1", "ws1");
        // Directly set ARCHIVED status
        visit.setStatus(VisitStatus.ARCHIVED);
        visitRepo.save(visit);

        assertThrows(BusinessRuleException.class, () ->
                visitService.closeVisit(clinicianUser, visit.getId(),
                        "close-arch-key-" + System.nanoTime(), "127.0.0.1", "ws1"));
    }
}
