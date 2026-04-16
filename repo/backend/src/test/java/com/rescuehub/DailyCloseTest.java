package com.rescuehub;

import com.rescuehub.entity.*;
import com.rescuehub.enums.*;
import com.rescuehub.exception.BusinessRuleException;
import com.rescuehub.repository.*;
import com.rescuehub.service.BillingService;
import com.rescuehub.service.DailyCloseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class DailyCloseTest extends BaseIntegrationTest {

    @Autowired private DailyCloseService dailyCloseService;
    @Autowired private BillingService billingService;
    @Autowired private InvoiceRepository invoiceRepo;
    @Autowired private PatientRepository patientRepo;
    @Autowired private VisitRepository visitRepo;

    @Test
    @Transactional
    void voidRejectedAfterDailyClose() {
        LocalDate testDate = LocalDate.of(2025, 1, 15);

        // Create a patient and visit
        Patient patient = new Patient();
        patient.setOrganizationId(testOrg.getId());
        patient.setMedicalRecordNumber("DC-TEST-" + System.currentTimeMillis());
        patient.setDateOfBirth(LocalDate.of(1980, 1, 1));
        patient = patientRepo.save(patient);

        Visit visit = new Visit();
        visit.setOrganizationId(testOrg.getId());
        visit.setPatientId(patient.getId());
        visit.setCreatedByUserId(billingUser.getId());
        visit.setOpenedAt(Instant.now());
        visit.setStatus(VisitStatus.CLOSED);
        visit.setClosedAt(Instant.now());
        visit = visitRepo.save(visit);

        Invoice invoice = billingService.generateInvoiceForVisit(billingUser, visit, "127.0.0.1", "ws1");

        // Close the day
        dailyCloseService.close(billingUser, testDate, "127.0.0.1", "ws1");

        // Set the invoice's close date to testDate so void check triggers
        invoice.setDailyCloseDate(testDate);
        invoiceRepo.save(invoice);

        Long invoiceId = invoice.getId();
        assertThrows(BusinessRuleException.class, () ->
                billingService.voidInvoice(billingUser, invoiceId, "void-dc-key", "127.0.0.1", "ws1"));
    }

    @Test
    @Transactional
    void dailyCloseRequiresBillingRole() {
        assertThrows(com.rescuehub.exception.ForbiddenException.class, () ->
                dailyCloseService.close(frontDeskUser, LocalDate.now().minusDays(10), "127.0.0.1", "ws1"));
    }

    @Test
    @Transactional
    void duplicateDailyCloseFails() {
        LocalDate uniqueDate = LocalDate.of(2025, 2, 1);
        dailyCloseService.close(billingUser, uniqueDate, "127.0.0.1", "ws1");
        assertThrows(BusinessRuleException.class, () ->
                dailyCloseService.close(billingUser, uniqueDate, "127.0.0.1", "ws1"));
    }

    @Test
    @Transactional
    void list_returnsClosesForOrg() {
        LocalDate d = LocalDate.of(2025, 3, 10);
        dailyCloseService.close(billingUser, d, "127.0.0.1", "ws1");

        Page<DailyClose> page = dailyCloseService.list(billingUser, PageRequest.of(0, 20));
        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 1);
        assertTrue(page.getContent().stream().anyMatch(dc -> dc.getBusinessDate().equals(d)));
    }

    @Test
    @Transactional
    void list_clinicianForbidden() {
        assertThrows(com.rescuehub.exception.ForbiddenException.class, () ->
                dailyCloseService.list(clinicianUser, PageRequest.of(0, 20)));
    }
}
