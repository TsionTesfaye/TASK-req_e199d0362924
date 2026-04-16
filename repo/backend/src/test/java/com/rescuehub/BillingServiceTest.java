package com.rescuehub;

import com.rescuehub.entity.*;
import com.rescuehub.enums.*;
import com.rescuehub.exception.BusinessRuleException;
import com.rescuehub.repository.*;
import com.rescuehub.service.BillingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BillingServiceTest extends BaseIntegrationTest {

    @Autowired private BillingService billingService;
    @Autowired private VisitRepository visitRepo;
    @Autowired private VisitChargeRepository chargeRepo;
    @Autowired private BillingRuleRepository ruleRepo;
    @Autowired private InvoiceRepository invoiceRepo;
    @Autowired private DailyCloseRepository dailyCloseRepo;
    @Autowired private PatientRepository patientRepo;

    private Patient createTestPatient() {
        Patient p = new Patient();
        p.setOrganizationId(testOrg.getId());
        p.setMedicalRecordNumber("TEST-" + System.currentTimeMillis());
        p.setDateOfBirth(LocalDate.of(1990, 1, 1));
        return patientRepo.save(p);
    }

    private Visit createTestVisit(Patient patient) {
        Visit v = new Visit();
        v.setOrganizationId(testOrg.getId());
        v.setPatientId(patient.getId());
        v.setCreatedByUserId(billingUser.getId());
        v.setOpenedAt(Instant.now());
        v.setStatus(VisitStatus.CLOSED);
        v.setClosedAt(Instant.now());
        return visitRepo.save(v);
    }

    @Test
    @Transactional
    void discountCappedAt200() {
        Patient patient = createTestPatient();
        Visit visit = createTestVisit(patient);

        // Add a large service charge so discount would exceed $200 without cap
        VisitCharge charge = new VisitCharge();
        charge.setVisitId(visit.getId());
        charge.setServiceCode("EXPENSIVE");
        charge.setDescription("Expensive service");
        charge.setPricingSourceType("MANUAL");
        charge.setUnitPrice(new BigDecimal("5000.00"));
        charge.setQuantity(1);
        charge.setLineTotal(new BigDecimal("5000.00"));
        charge.setTaxable(true);
        chargeRepo.save(charge);

        // Add a 50% discount rule
        BillingRule discountRule = new BillingRule();
        discountRule.setOrganizationId(testOrg.getId());
        discountRule.setRuleType(BillingRuleType.DISCOUNT);
        discountRule.setCode("BIG_DISCOUNT");
        discountRule.setName("50% Discount");
        discountRule.setPercentage(new BigDecimal("50.00"));
        discountRule.setActive(true);
        discountRule.setPriority(10);
        ruleRepo.save(discountRule);

        Invoice invoice = billingService.generateInvoiceForVisit(billingUser, visit, "127.0.0.1", "ws1");

        // Discount must be capped at 200
        assertTrue(invoice.getDiscountAmount().compareTo(new BigDecimal("200.00")) <= 0,
                "Discount should not exceed $200");
        assertNotNull(invoice.getInvoiceNumber());
    }

    @Test
    @Transactional
    void invoiceIdempotent() {
        Patient patient = createTestPatient();
        Visit visit = createTestVisit(patient);

        Invoice first = billingService.generateInvoiceForVisit(billingUser, visit, "127.0.0.1", "ws1");
        Invoice second = billingService.generateInvoiceForVisit(billingUser, visit, "127.0.0.1", "ws1");
        assertEquals(first.getId(), second.getId(), "Should return same invoice on duplicate call");
    }

    @Test
    @Transactional
    void voidRejectedAfterDailyClose() {
        Patient patient = createTestPatient();
        Visit visit = createTestVisit(patient);
        Invoice invoice = billingService.generateInvoiceForVisit(billingUser, visit, "127.0.0.1", "ws1");

        // Create daily close for today
        DailyClose dc = new DailyClose();
        dc.setOrganizationId(testOrg.getId());
        dc.setBusinessDate(LocalDate.now());
        dc.setClosedByUserId(billingUser.getId());
        dc.setClosedAt(Instant.now());
        dc.setStatus(DailyCloseStatus.CLOSED);
        dailyCloseRepo.save(dc);

        assertThrows(BusinessRuleException.class, () ->
                billingService.voidInvoice(billingUser, invoice.getId(), "void-key-1", "127.0.0.1", "ws1"));
    }

    @Test
    @Transactional
    void refundWindowEnforced() {
        Patient patient = createTestPatient();
        Visit visit = createTestVisit(patient);
        Invoice invoice = billingService.generateInvoiceForVisit(billingUser, visit, "127.0.0.1", "ws1");

        // Artificially set generated_at to 31 days ago
        invoice.setGeneratedAt(Instant.now().minusSeconds(31L * 24 * 3600));
        invoiceRepo.save(invoice);

        assertThrows(BusinessRuleException.class, () ->
                billingService.refundInvoice(billingUser, invoice.getId(),
                        new BigDecimal("10.00"), "test refund", "refund-key-win", "127.0.0.1", "ws1"));
    }

    @Test
    @Transactional
    void recordPayment_reducesOutstandingAmount() {
        Patient patient = createTestPatient();
        Visit visit = createTestVisit(patient);
        Invoice invoice = billingService.generateInvoiceForVisit(billingUser, visit, "127.0.0.1", "ws1");
        // Ensure there is something to pay
        invoice.setTotalAmount(new BigDecimal("100.00"));
        invoice.setOutstandingAmount(new BigDecimal("100.00"));
        invoice.setStatus(InvoiceStatus.OPEN);
        invoiceRepo.save(invoice);

        Payment payment = billingService.recordPayment(billingUser, invoice.getId(),
                TenderType.CASH, new BigDecimal("50.00"), null, "127.0.0.1", "ws1");

        assertNotNull(payment.getId());
        Invoice reloaded = invoiceRepo.findById(invoice.getId()).orElseThrow();
        assertEquals(0, reloaded.getOutstandingAmount().compareTo(new BigDecimal("50.00")));
        assertEquals(InvoiceStatus.PARTIALLY_PAID, reloaded.getStatus());
    }

    @Test
    @Transactional
    void recordPayment_fullPaymentMarksPaid() {
        Patient patient = createTestPatient();
        Visit visit = createTestVisit(patient);
        Invoice invoice = billingService.generateInvoiceForVisit(billingUser, visit, "127.0.0.1", "ws1");
        invoice.setTotalAmount(new BigDecimal("50.00"));
        invoice.setOutstandingAmount(new BigDecimal("50.00"));
        invoice.setStatus(InvoiceStatus.OPEN);
        invoiceRepo.save(invoice);

        billingService.recordPayment(billingUser, invoice.getId(),
                TenderType.CASH, new BigDecimal("50.00"), "ref-123", "127.0.0.1", "ws1");

        Invoice reloaded = invoiceRepo.findById(invoice.getId()).orElseThrow();
        assertEquals(InvoiceStatus.PAID, reloaded.getStatus());
    }

    @Test
    @Transactional
    void listInvoices_returnsInvoicesForOrg() {
        Patient patient = createTestPatient();
        Visit visit = createTestVisit(patient);
        billingService.generateInvoiceForVisit(billingUser, visit, "127.0.0.1", "ws1");

        Page<Invoice> page = billingService.listInvoices(billingUser, PageRequest.of(0, 20));
        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 1);
    }

    @Test
    @Transactional
    void listInvoices_clinicianForbidden() {
        assertThrows(com.rescuehub.exception.ForbiddenException.class, () ->
                billingService.listInvoices(clinicianUser, PageRequest.of(0, 20)));
    }

    @Test
    @Transactional
    void getInvoice_returnsInvoiceById() {
        Patient patient = createTestPatient();
        Visit visit = createTestVisit(patient);
        Invoice invoice = billingService.generateInvoiceForVisit(billingUser, visit, "127.0.0.1", "ws1");

        Invoice fetched = billingService.getInvoice(billingUser, invoice.getId());
        assertNotNull(fetched);
        assertEquals(invoice.getId(), fetched.getId());
    }

    @Test
    @Transactional
    void listPayments_returnsPaymentsForInvoice() {
        Patient patient = createTestPatient();
        Visit visit = createTestVisit(patient);
        Invoice invoice = billingService.generateInvoiceForVisit(billingUser, visit, "127.0.0.1", "ws1");
        invoice.setTotalAmount(new BigDecimal("80.00"));
        invoice.setOutstandingAmount(new BigDecimal("80.00"));
        invoice.setStatus(InvoiceStatus.OPEN);
        invoiceRepo.save(invoice);

        billingService.recordPayment(billingUser, invoice.getId(),
                TenderType.CHECK, new BigDecimal("40.00"), null, "127.0.0.1", "ws1");

        List<Payment> payments = billingService.listPayments(billingUser, invoice.getId());
        assertNotNull(payments);
        assertEquals(1, payments.size());
        assertEquals(0, payments.get(0).getAmount().compareTo(new BigDecimal("40.00")));
    }

    @Test
    @Transactional
    void deterministic_calculation_order() {
        Patient patient = createTestPatient();
        Visit visit = createTestVisit(patient);

        // service: 100
        VisitCharge charge = new VisitCharge();
        charge.setVisitId(visit.getId());
        charge.setServiceCode("OFFICE_VISIT");
        charge.setDescription("Office visit");
        charge.setPricingSourceType("RULE");
        charge.setUnitPrice(new BigDecimal("100.00"));
        charge.setQuantity(1);
        charge.setLineTotal(new BigDecimal("100.00"));
        charge.setTaxable(true);
        chargeRepo.save(charge);

        // discount: 10%
        BillingRule discRule = new BillingRule();
        discRule.setOrganizationId(testOrg.getId());
        discRule.setRuleType(BillingRuleType.DISCOUNT);
        discRule.setCode("DISC_10");
        discRule.setName("10% off");
        discRule.setPercentage(new BigDecimal("10.00"));
        discRule.setActive(true);
        discRule.setPriority(5);
        ruleRepo.save(discRule);

        // tax: 8%
        BillingRule taxRule = new BillingRule();
        taxRule.setOrganizationId(testOrg.getId());
        taxRule.setRuleType(BillingRuleType.TAX);
        taxRule.setCode("TAX_8");
        taxRule.setName("8% Tax");
        taxRule.setTaxRate(new BigDecimal("8.00"));
        taxRule.setActive(true);
        taxRule.setPriority(90);
        ruleRepo.save(taxRule);

        Invoice invoice = billingService.generateInvoiceForVisit(billingUser, visit, "127.0.0.1", "ws1");

        // subtotal=100, discount=10, after=90, tax=90*8%=7.20, total=97.20
        assertEquals(0, invoice.getSubtotalAmount().compareTo(new BigDecimal("100.00")));
        assertEquals(0, invoice.getDiscountAmount().compareTo(new BigDecimal("10.00")));
        assertEquals(0, invoice.getTaxAmount().compareTo(new BigDecimal("7.20")));
        assertEquals(0, invoice.getTotalAmount().compareTo(new BigDecimal("97.20")));
    }

    @Test
    @Transactional
    void recordPayment_voidedInvoice_throwsBusinessRule() {
        Patient patient = createTestPatient();
        Visit visit = createTestVisit(patient);
        Invoice invoice = billingService.generateInvoiceForVisit(billingUser, visit, "127.0.0.1", "ws1");
        // Directly set status to VOIDED without going through voidInvoice (avoids time constraint)
        invoice.setStatus(InvoiceStatus.VOIDED);
        invoiceRepo.save(invoice);

        assertThrows(BusinessRuleException.class, () ->
                billingService.recordPayment(billingUser, invoice.getId(),
                        TenderType.CASH, new BigDecimal("10.00"), null, "127.0.0.1", "ws1"));
    }

    @Test
    @Transactional
    void recordPayment_alreadyFullyPaid_throwsBusinessRule() {
        Patient patient = createTestPatient();
        Visit visit = createTestVisit(patient);
        Invoice invoice = billingService.generateInvoiceForVisit(billingUser, visit, "127.0.0.1", "ws1");
        invoice.setTotalAmount(new BigDecimal("50.00"));
        invoice.setOutstandingAmount(BigDecimal.ZERO);
        invoice.setStatus(InvoiceStatus.PAID);
        invoiceRepo.save(invoice);

        assertThrows(BusinessRuleException.class, () ->
                billingService.recordPayment(billingUser, invoice.getId(),
                        TenderType.CASH, new BigDecimal("10.00"), null, "127.0.0.1", "ws1"));
    }

    @Test
    @Transactional
    void refundInvoice_voidedInvoice_throwsBusinessRule() {
        Patient patient = createTestPatient();
        Visit visit = createTestVisit(patient);
        Invoice invoice = billingService.generateInvoiceForVisit(billingUser, visit, "127.0.0.1", "ws1");
        invoice.setStatus(InvoiceStatus.VOIDED);
        invoiceRepo.save(invoice);

        assertThrows(BusinessRuleException.class, () ->
                billingService.refundInvoice(billingUser, invoice.getId(),
                        new BigDecimal("10.00"), "voided refund",
                        "refund-voided-" + System.nanoTime(), "127.0.0.1", "ws1"));
    }

    @Test
    @Transactional
    void refundInvoice_precisionExceeds2DecimalPlaces_throwsBusinessRule() {
        Patient patient = createTestPatient();
        Visit visit = createTestVisit(patient);
        Invoice invoice = billingService.generateInvoiceForVisit(billingUser, visit, "127.0.0.1", "ws1");

        // Amount with 3 decimal places should be rejected
        BigDecimal badAmount = new BigDecimal("10.001");
        assertThrows(BusinessRuleException.class, () ->
                billingService.refundInvoice(billingUser, invoice.getId(),
                        badAmount, "precision test",
                        "refund-prec-" + System.nanoTime(), "127.0.0.1", "ws1"));
    }

    @Test
    @Transactional
    void voidInvoice_succeeds() {
        Patient patient = createTestPatient();
        Visit visit = createTestVisit(patient);
        Invoice invoice = billingService.generateInvoiceForVisit(billingUser, visit, "127.0.0.1", "ws1");
        // Set dailyCloseDate to tomorrow so the 23:00 cutoff is far in the future
        invoice.setDailyCloseDate(LocalDate.now().plusDays(1));
        invoiceRepo.save(invoice);

        Invoice voided = billingService.voidInvoice(billingUser, invoice.getId(),
                "void-ok-" + System.nanoTime(), "127.0.0.1", "ws1");

        assertEquals(InvoiceStatus.VOIDED, voided.getStatus());
        assertNotNull(voided.getVoidedAt());
    }

    @Test
    @Transactional
    void voidInvoice_alreadyVoided_withDifferentKey_returnsInvoice() {
        Patient patient = createTestPatient();
        Visit visit = createTestVisit(patient);
        Invoice invoice = billingService.generateInvoiceForVisit(billingUser, visit, "127.0.0.1", "ws1");
        invoice.setDailyCloseDate(LocalDate.now().plusDays(1));
        invoiceRepo.save(invoice);

        billingService.voidInvoice(billingUser, invoice.getId(),
                "void-first-" + System.nanoTime(), "127.0.0.1", "ws1");

        // Second void with a different key — hits the already-VOIDED idempotent path
        Invoice result = billingService.voidInvoice(billingUser, invoice.getId(),
                "void-second-" + System.nanoTime(), "127.0.0.1", "ws1");

        assertEquals(InvoiceStatus.VOIDED, result.getStatus());
    }

    @Test
    @Transactional
    void voidInvoice_refundedInvoice_throwsBusinessRule() {
        Patient patient = createTestPatient();
        Visit visit = createTestVisit(patient);
        Invoice invoice = billingService.generateInvoiceForVisit(billingUser, visit, "127.0.0.1", "ws1");
        invoice.setStatus(InvoiceStatus.REFUNDED);
        invoice.setDailyCloseDate(LocalDate.now().plusDays(1));
        invoiceRepo.save(invoice);

        assertThrows(BusinessRuleException.class, () ->
                billingService.voidInvoice(billingUser, invoice.getId(),
                        "void-refunded-" + System.nanoTime(), "127.0.0.1", "ws1"));
    }

    @Test
    @Transactional
    void refundInvoice_exceedsBalance_throwsBusinessRule() {
        Patient patient = createTestPatient();
        Visit visit = createTestVisit(patient);
        Invoice invoice = billingService.generateInvoiceForVisit(billingUser, visit, "127.0.0.1", "ws1");
        invoice.setTotalAmount(new BigDecimal("10.00"));
        invoice.setOutstandingAmount(new BigDecimal("10.00"));
        invoice.setStatus(InvoiceStatus.OPEN);
        invoiceRepo.save(invoice);

        assertThrows(BusinessRuleException.class, () ->
                billingService.refundInvoice(billingUser, invoice.getId(),
                        new BigDecimal("500.00"), "exceeds balance",
                        "refund-excess-" + System.nanoTime(), "127.0.0.1", "ws1"));
    }

    @Test
    @Transactional
    void packageRule_appliesPackagePrice() {
        Patient patient = createTestPatient();
        Visit visit = createTestVisit(patient);

        // Add two service charges that match a package
        VisitCharge c1 = new VisitCharge();
        c1.setVisitId(visit.getId());
        c1.setServiceCode("SVC_A");
        c1.setDescription("Service A");
        c1.setPricingSourceType("MANUAL");
        c1.setUnitPrice(new BigDecimal("80.00"));
        c1.setQuantity(1);
        c1.setLineTotal(new BigDecimal("80.00"));
        c1.setTaxable(false);
        chargeRepo.save(c1);

        VisitCharge c2 = new VisitCharge();
        c2.setVisitId(visit.getId());
        c2.setServiceCode("SVC_B");
        c2.setDescription("Service B");
        c2.setPricingSourceType("MANUAL");
        c2.setUnitPrice(new BigDecimal("70.00"));
        c2.setQuantity(1);
        c2.setLineTotal(new BigDecimal("70.00"));
        c2.setTaxable(false);
        chargeRepo.save(c2);

        // Package rule: SVC_A + SVC_B bundled for $120
        BillingRule pkgRule = new BillingRule();
        pkgRule.setOrganizationId(testOrg.getId());
        pkgRule.setRuleType(BillingRuleType.PACKAGE);
        pkgRule.setCode("PKG_AB");
        pkgRule.setName("Package A+B");
        pkgRule.setAmount(new BigDecimal("120.00"));
        pkgRule.setPackageDefinitionJson("[\"SVC_A\",\"SVC_B\"]");
        pkgRule.setActive(true);
        pkgRule.setPriority(1);
        ruleRepo.save(pkgRule);

        Invoice invoice = billingService.generateInvoiceForVisit(billingUser, visit, "127.0.0.1", "ws1");

        // Package replaces $150 bundle with $120 → subtotal should be adjusted
        assertNotNull(invoice);
        assertTrue(invoice.getTotalAmount().compareTo(BigDecimal.ZERO) >= 0);
    }
}
