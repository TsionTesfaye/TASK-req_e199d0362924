package com.rescuehub;

import com.rescuehub.entity.*;
import com.rescuehub.enums.*;
import com.rescuehub.repository.*;
import com.rescuehub.service.VisitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class VisitCloseTest extends BaseIntegrationTest {

    @Autowired private VisitService visitService;
    @Autowired private PatientRepository patientRepo;
    @Autowired private InvoiceRepository invoiceRepo;

    @Test
    @Transactional
    void visitCloseGeneratesInvoice() {
        Patient patient = new Patient();
        patient.setOrganizationId(testOrg.getId());
        patient.setMedicalRecordNumber("VC-TEST-" + System.currentTimeMillis());
        patient.setDateOfBirth(LocalDate.of(1985, 6, 15));
        patient = patientRepo.save(patient);

        String idempKey = "open-" + UUID.randomUUID();
        Visit visit = visitService.open(clinicianUser, patient.getId(), null, "Headache",
                idempKey, "127.0.0.1", "ws1");

        assertNotNull(visit.getId());
        assertEquals(VisitStatus.OPEN, visit.getStatus());

        String closeKey = "close-" + UUID.randomUUID();
        Visit closed = visitService.closeVisit(clinicianUser, visit.getId(), closeKey, "127.0.0.1", "ws1");

        assertEquals(VisitStatus.CLOSED, closed.getStatus());
        assertNotNull(closed.getClosedAt());

        // Invoice must exist
        assertTrue(invoiceRepo.existsByVisitId(closed.getId()), "Invoice should be generated after visit close");

        Invoice invoice = invoiceRepo.findByVisitId(closed.getId()).orElseThrow();
        assertNotNull(invoice.getInvoiceNumber());
        assertTrue(invoice.getTotalAmount().compareTo(java.math.BigDecimal.ZERO) >= 0);
    }

    @Test
    void visitCloseIdempotent() {
        Patient patient = new Patient();
        patient.setOrganizationId(testOrg.getId());
        patient.setMedicalRecordNumber("VC-IDEM-" + System.currentTimeMillis());
        patient.setDateOfBirth(LocalDate.of(1990, 3, 20));
        patient = patientRepo.save(patient);

        Visit visit = visitService.open(clinicianUser, patient.getId(), null, "Back pain",
                null, "127.0.0.1", "ws1");

        String closeKey = "close-idem-" + UUID.randomUUID();
        Visit closed1 = visitService.closeVisit(clinicianUser, visit.getId(), closeKey, "127.0.0.1", "ws1");
        Visit closed2 = visitService.closeVisit(clinicianUser, visit.getId(), closeKey, "127.0.0.1", "ws1");

        assertEquals(closed1.getId(), closed2.getId());
        assertEquals(VisitStatus.CLOSED, closed2.getStatus());
        // Only one invoice should exist
        assertEquals(1, invoiceRepo.findByVisitId(visit.getId()).isPresent() ? 1 : 0);
    }
}
