package com.rescuehub;

import com.rescuehub.entity.*;
import com.rescuehub.enums.*;
import com.rescuehub.exception.BusinessRuleException;
import com.rescuehub.exception.ForbiddenException;
import com.rescuehub.repository.*;
import com.rescuehub.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class HardeningTest extends BaseIntegrationTest {

    @Autowired PatientService patientService;
    @Autowired VisitService visitService;
    @Autowired BillingService billingService;
    @Autowired ExportService exportService;
    @Autowired AuditLogRepository auditRepo;
    @Autowired InvoiceRepository invoiceRepo;
    @Autowired PatientRepository patientRepo;

    private Patient makePatient() {
        return patientService.register(frontDeskUser, "Test", "Case" + System.nanoTime(),
                LocalDate.of(1990, 1, 1), "F", "5550001", "1 Test St",
                null, null, false, false, "127.0.0.1", "ws");
    }

    @Test
    @Transactional
    void revealLogsAuditEventWithFieldsList() {
        Patient p = makePatient();
        long auditBefore = auditRepo.count();

        PatientService.RevealResult out = patientService.reveal(billingUser, p.getId(), "127.0.0.1", "ws");
        assertNotNull(out);

        long auditAfter = auditRepo.count();
        assertEquals(auditBefore + 1, auditAfter, "reveal must write exactly one audit row");

        List<AuditLog> all = auditRepo.findAll();
        AuditLog last = all.get(all.size() - 1);
        assertEquals("PATIENT_PII_REVEAL", last.getActionCode());
        assertNotNull(last.getAfterJson());
        assertTrue(last.getAfterJson().contains("fieldsRevealed"), "audit payload must enumerate fields");
        assertEquals(billingUser.getId(), last.getActorUserId());
    }

    @Test
    @Transactional
    void protectedCasePatientBlockedFromNonPrivilegedReveal() {
        Patient p = patientService.register(frontDeskUser, "Prot", "Case" + System.nanoTime(),
                LocalDate.of(1995, 3, 3), "M", "5550002", "2 Protected Ln",
                null, null, false, true, "127.0.0.1", "ws");
        assertThrows(ForbiddenException.class,
                () -> patientService.reveal(billingUser, p.getId(), "127.0.0.1", "ws"));
        // Admin is permitted
        assertNotNull(patientService.reveal(adminUser, p.getId(), "127.0.0.1", "ws"));
    }

    @Test
    @Transactional
    void refundRequiresIdempotencyKey() {
        Patient p = makePatient();
        Visit v = visitService.open(clinicianUser, p.getId(), null, "test",
                "visit-idem-" + System.nanoTime(), "127.0.0.1", "ws");
        // close → invoice
        visitService.closeVisit(clinicianUser, v.getId(),
                "close-idem-" + System.nanoTime(), "127.0.0.1", "ws");
        Invoice inv = invoiceRepo.findByVisitId(v.getId()).orElseThrow();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                billingService.refundInvoice(billingUser, inv.getId(),
                        new BigDecimal("1.00"), "test", null, "127.0.0.1", "ws"));
        assertTrue(ex.getMessage().toLowerCase().contains("idempotency"));
    }

    @Test
    @Transactional
    void refundIdempotencyCollisionIsRejected() {
        Patient p = makePatient();
        Visit v = visitService.open(clinicianUser, p.getId(), null, "test",
                "visit-idem-" + System.nanoTime(), "127.0.0.1", "ws");
        visitService.closeVisit(clinicianUser, v.getId(),
                "close-idem-" + System.nanoTime(), "127.0.0.1", "ws");
        Invoice inv = invoiceRepo.findByVisitId(v.getId()).orElseThrow();
        // Ensure a positive balance regardless of seed billing rules
        inv.setTotalAmount(new BigDecimal("50.00"));
        invoiceRepo.save(inv);

        String key = "refund-collision-" + System.nanoTime();
        billingService.refundInvoice(billingUser, inv.getId(),
                new BigDecimal("1.00"), "first", key, "127.0.0.1", "ws");
        assertThrows(BusinessRuleException.class, () ->
                billingService.refundInvoice(billingUser, inv.getId(),
                        new BigDecimal("1.00"), "dup", key, "127.0.0.1", "ws"));
    }

    @Test
    @Transactional
    void exportThresholdRejectedWithoutElevation() {
        // Seed >500 ledger entries by inserting patients (count driven off real repo). We use
        // a direct count assertion on a small data set → no rejection expected for small volume.
        // To test the rejection path, pass the "patients" type and seed 501 patient rows.
        for (int i = 0; i < 501; i++) {
            patientService.register(frontDeskUser, "Bulk", "P" + i + "_" + System.nanoTime(),
                    LocalDate.of(1980, 1, 1), "F", null, null, null, null, false, false,
                    "127.0.0.1", "ws");
        }
        String key = "export-over-" + System.nanoTime();
        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                exportService.export(adminUser, "patients", key, false, false, "127.0.0.1", "ws"));
        assertTrue(ex.getMessage().toLowerCase().contains("elevated"));
    }

    @Test
    @Transactional
    void refundRoleEnforced_clinicianCannotRefund() {
        Patient p = makePatient();
        Visit v = visitService.open(clinicianUser, p.getId(), null, "test",
                "visit-rle-" + System.nanoTime(), "127.0.0.1", "ws");
        visitService.closeVisit(clinicianUser, v.getId(),
                "close-rle-" + System.nanoTime(), "127.0.0.1", "ws");
        Invoice inv = invoiceRepo.findByVisitId(v.getId()).orElseThrow();
        inv.setTotalAmount(new BigDecimal("50.00"));
        invoiceRepo.save(inv);

        assertThrows(ForbiddenException.class, () ->
                billingService.refundInvoice(clinicianUser, inv.getId(),
                        new BigDecimal("5.00"), "x", "rle-key", "127.0.0.1", "ws"));
    }

    @Test
    @Transactional
    void refundPrecisionRejected() {
        Patient p = makePatient();
        Visit v = visitService.open(clinicianUser, p.getId(), null, "test",
                "visit-prec-" + System.nanoTime(), "127.0.0.1", "ws");
        visitService.closeVisit(clinicianUser, v.getId(),
                "close-prec-" + System.nanoTime(), "127.0.0.1", "ws");
        Invoice inv = invoiceRepo.findByVisitId(v.getId()).orElseThrow();
        inv.setTotalAmount(new BigDecimal("50.00"));
        invoiceRepo.save(inv);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                billingService.refundInvoice(billingUser, inv.getId(),
                        new BigDecimal("1.234"), "x", "prec-key", "127.0.0.1", "ws"));
        assertTrue(ex.getMessage().toLowerCase().contains("precision"));
    }

    @Test
    @Transactional
    void voidAfterRefundRejected() {
        Patient p = makePatient();
        Visit v = visitService.open(clinicianUser, p.getId(), null, "test",
                "visit-var-" + System.nanoTime(), "127.0.0.1", "ws");
        visitService.closeVisit(clinicianUser, v.getId(),
                "close-var-" + System.nanoTime(), "127.0.0.1", "ws");
        Invoice inv = invoiceRepo.findByVisitId(v.getId()).orElseThrow();
        inv.setTotalAmount(new BigDecimal("50.00"));
        invoiceRepo.save(inv);

        billingService.refundInvoice(billingUser, inv.getId(),
                new BigDecimal("50.00"), "full", "rfd-" + System.nanoTime(), "127.0.0.1", "ws");

        assertThrows(BusinessRuleException.class, () ->
                billingService.voidInvoice(billingUser, inv.getId(),
                        "void-after-refund", "127.0.0.1", "ws"));
    }

    @Test
    @Transactional
    void deniedReveal_logsAuditWithReason() {
        Patient p = makePatient();
        long before = auditRepo.count();
        assertThrows(ForbiddenException.class,
                () -> patientService.reveal(moderatorUser(), p.getId(), "127.0.0.1", "ws"));
        long after = auditRepo.count();
        assertEquals(before + 1, after, "denied reveal must still produce audit row");
        var all = auditRepo.findAll();
        var last = all.get(all.size() - 1);
        assertEquals("PATIENT_PII_REVEAL_DENIED", last.getActionCode());
        assertTrue(last.getAfterJson().contains("role_not_permitted"));
    }

    private User moderatorUser() {
        return getOrCreateUser("test_moderator", Role.MODERATOR);
    }

    @Autowired AppointmentService appointmentService;
    @Autowired ShelterService shelterService;
    @Autowired CorrectiveActionService correctiveActionService;

    // ── B1: Role-enforcement tests ────────────────────────────────────────────

    @Test
    @Transactional
    void billing_cannotOpenVisit() {
        Patient p = makePatient();
        assertThrows(ForbiddenException.class, () ->
                visitService.open(billingUser, p.getId(), null, "test",
                        "idem-billing-" + System.nanoTime(), "127.0.0.1", "ws"));
    }

    @Test
    @Transactional
    void billing_cannotCreateAppointment() {
        assertThrows(ForbiddenException.class, () ->
                appointmentService.create(billingUser, 1L, LocalDate.now(), java.time.LocalTime.NOON,
                        clinicianUser.getId(), "127.0.0.1", "ws"));
    }

    @Test
    @Transactional
    void clinician_cannotCreateCorrectiveAction() {
        assertThrows(ForbiddenException.class, () ->
                correctiveActionService.create(clinicianUser, "test desc", null, null,
                        "127.0.0.1", "ws"));
    }

    @Test
    @Transactional
    void frontDesk_cannotCreateShelterResource() {
        assertThrows(ForbiddenException.class, () ->
                shelterService.create(frontDeskUser, "Test Shelter", "FOOD", "Downtown",
                        "1 Main St", null, null, "127.0.0.1", "ws"));
    }

    @Test
    @Transactional
    void moderator_cannotRegisterPatient() {
        User mod = getOrCreateUser("test_moderator_b1", Role.MODERATOR);
        assertThrows(ForbiddenException.class, () ->
                patientService.register(mod, "Jane", "Doe", LocalDate.of(1990, 1, 1),
                        "F", null, null, null, null, false, false, "127.0.0.1", "ws"));
    }

    @Test
    @Transactional
    void crossOrg_patientAccessRejected() {
        // Create patient in testOrg
        Patient p = makePatient();

        // Create a second org and a user in it
        Organization otherOrg = new Organization();
        otherOrg.setCode("OTHER-" + System.nanoTime());
        otherOrg.setName("Other Org");
        otherOrg.setActive(true);
        otherOrg = orgRepo.save(otherOrg);

        User otherAdmin = new User();
        otherAdmin.setOrganizationId(otherOrg.getId());
        otherAdmin.setUsername("other_admin_" + System.nanoTime());
        otherAdmin.setPasswordHash("x");
        otherAdmin.setDisplayName("Other Admin");
        otherAdmin.setRole(Role.ADMIN);
        otherAdmin.setActive(true);
        otherAdmin.setFrozen(false);
        otherAdmin.setPasswordChangedAt(java.time.Instant.now());
        otherAdmin = userRepo.save(otherAdmin);

        // Access from other org must be rejected
        final User finalOtherAdmin = otherAdmin;
        final Long patientId = p.getId();
        assertThrows(com.rescuehub.exception.NotFoundException.class, () ->
                patientService.getById(finalOtherAdmin, patientId));
    }

    @Autowired QualityRulesService qualityRulesService;

    // ── B1: Read/list role-enforcement tests ─────────────────────────────────

    @Test
    @Transactional
    void billing_cannotListAppointments() {
        assertThrows(ForbiddenException.class, () ->
                appointmentService.list(billingUser, null,
                        org.springframework.data.domain.PageRequest.of(0, 10)));
    }

    @Test
    @Transactional
    void billing_cannotGetVisitById() {
        Patient p = makePatient();
        Visit v = visitService.open(clinicianUser, p.getId(), null, "test",
                "idem-visit-read-" + System.nanoTime(), "127.0.0.1", "ws");
        assertThrows(ForbiddenException.class, () ->
                visitService.getById(billingUser, v.getId()));
    }

    @Test
    @Transactional
    void clinician_cannotListQualityResults() {
        assertThrows(ForbiddenException.class, () ->
                qualityRulesService.listResults(clinicianUser,
                        org.springframework.data.domain.PageRequest.of(0, 10)));
    }

    @Test
    @Transactional
    void clinician_cannotListCorrectiveActions() {
        assertThrows(ForbiddenException.class, () ->
                correctiveActionService.list(clinicianUser,
                        org.springframework.data.domain.PageRequest.of(0, 10)));
    }

    @Test
    @Transactional
    void clinician_canListAppointments() {
        // Valid role — must not throw
        assertDoesNotThrow(() ->
                appointmentService.list(clinicianUser, null,
                        org.springframework.data.domain.PageRequest.of(0, 10)));
    }

    @Test
    @Transactional
    void quality_canListCorrectiveActions() {
        assertDoesNotThrow(() ->
                correctiveActionService.list(qualityUser,
                        org.springframework.data.domain.PageRequest.of(0, 10)));
    }

    @Test
    @Transactional
    void crossOrg_appointmentAccessRejected() {
        // Appointment created in testOrg — user from another org must not see it
        Patient p = makePatient();
        Appointment appt = appointmentService.create(frontDeskUser, p.getId(),
                LocalDate.now(), java.time.LocalTime.NOON, null, "127.0.0.1", "ws");

        Organization otherOrg = new Organization();
        otherOrg.setCode("X-" + System.nanoTime());
        otherOrg.setName("Cross Org");
        otherOrg.setActive(true);
        otherOrg = orgRepo.save(otherOrg);

        User otherAdmin = new User();
        otherAdmin.setOrganizationId(otherOrg.getId());
        otherAdmin.setUsername("x_admin_" + System.nanoTime());
        otherAdmin.setPasswordHash("x");
        otherAdmin.setDisplayName("X Admin");
        otherAdmin.setRole(Role.ADMIN);
        otherAdmin.setActive(true);
        otherAdmin.setFrozen(false);
        otherAdmin.setPasswordChangedAt(java.time.Instant.now());
        otherAdmin = userRepo.save(otherAdmin);

        final User finalOther = otherAdmin;
        final Long apptId = appt.getId();
        assertThrows(com.rescuehub.exception.NotFoundException.class, () ->
                appointmentService.getById(finalOther, apptId));
    }

    @Autowired IncidentService incidentService;
    @Autowired IncidentReportRepository incidentRepo;
    @Autowired CryptoService cryptoService;

    private IncidentReport submitNonProtectedIncident() {
        return incidentService.submit(frontDeskUser, "idm-inc-" + System.nanoTime(),
                "welfare", "description",
                "5th & Main", "Downtown", "5th & Main",
                null, false, false, false, "adult",
                "127.0.0.1", "ws");
    }

    @Test
    @Transactional
    void reclassifyNonProtected_encryptsLocation() {
        IncidentReport inc = submitNonProtectedIncident();
        assertNull(inc.getExactLocationCiphertext(), "non-protected submit must not store ciphertext");

        long auditBefore = auditRepo.count();
        IncidentReport reclassified = incidentService.reclassify(adminUser, inc.getId(),
                true, false, "123 Exact Address", "survivor privacy", "127.0.0.1", "ws");

        assertNotNull(reclassified.getExactLocationCiphertext(), "ciphertext must exist after reclassify");
        assertNotNull(reclassified.getExactLocationIv(), "iv must exist after reclassify");
        String plaintext = cryptoService.decrypt(
                reclassified.getExactLocationCiphertext(), reclassified.getExactLocationIv());
        assertEquals("123 Exact Address", plaintext);

        long auditAfter = auditRepo.count();
        assertEquals(auditBefore + 2, auditAfter,
                "reclassify must write exactly two audit rows: reclassify + encrypt");
        var all = auditRepo.findAll();
        assertEquals("INCIDENT_RECLASSIFY_ENCRYPT", all.get(all.size() - 1).getActionCode());
    }

    @Test
    @Transactional
    void reclassifyAlreadyProtected_noDoubleEncryption() {
        // Submit as protected — ciphertext stored at submit time
        IncidentReport inc = incidentService.submit(frontDeskUser, "idm-prot-" + System.nanoTime(),
                "welfare", "description", "5th & Main", "Downtown", "5th & Main",
                "999 Protected Ln", false, false, true, "adult",
                "127.0.0.1", "ws");
        byte[] originalCt = inc.getExactLocationCiphertext();
        byte[] originalIv = inc.getExactLocationIv();
        assertNotNull(originalCt);

        // Reclassify with a NEW plaintext — because ciphertext already present, no re-encryption
        long auditBefore = auditRepo.count();
        IncidentReport after = incidentService.reclassify(adminUser, inc.getId(),
                true, true, "different plaintext", "updated classification", "127.0.0.1", "ws");
        long auditAfter = auditRepo.count();

        assertArrayEquals(originalCt, after.getExactLocationCiphertext(),
                "ciphertext must not be overwritten when already encrypted");
        assertArrayEquals(originalIv, after.getExactLocationIv());
        assertEquals(auditBefore + 1, auditAfter,
                "only the reclassify audit row, no INCIDENT_RECLASSIFY_ENCRYPT");
    }

    @Test
    @Transactional
    void reclassifyNonSensitiveTarget_doesNotEncrypt() {
        IncidentReport inc = submitNonProtectedIncident();
        long auditBefore = auditRepo.count();

        IncidentReport after = incidentService.reclassify(adminUser, inc.getId(),
                false, false, "should not be stored", "no-op", "127.0.0.1", "ws");

        assertNull(after.getExactLocationCiphertext(),
                "non-sensitive reclassify must never store ciphertext");
        assertEquals(auditBefore + 1, auditRepo.count(),
                "only one audit row: the reclassify itself");
    }

    @Test
    void riskScoreServiceIsEphemeralAndNonEnforcing() {
        // Explicit semantics test: scores are in-memory, non-authoritative, and documented.
        RiskScoreService rs = new RiskScoreService();
        assertTrue(rs.isEphemeral());
        rs.recordEvent("ws-1", "ANOMALOUS_LOGIN");
        assertEquals(10, rs.getScore("ws-1"));
        // A fresh instance simulates a process restart → score is gone.
        RiskScoreService fresh = new RiskScoreService();
        assertEquals(0, fresh.getScore("ws-1"));
    }

    @Test
    void concurrentRefundsOnSameInvoiceOnlyOneSucceeds() throws Exception {
        Patient p = makePatient();
        Visit v = visitService.open(clinicianUser, p.getId(), null, "test",
                "visit-concur-" + System.nanoTime(), "127.0.0.1", "ws");
        visitService.closeVisit(clinicianUser, v.getId(),
                "close-concur-" + System.nanoTime(), "127.0.0.1", "ws");
        Invoice inv = invoiceRepo.findByVisitId(v.getId()).orElseThrow();
        inv.setTotalAmount(new BigDecimal("50.00"));
        invoiceRepo.save(inv);

        BigDecimal amount = new BigDecimal("1.00");

        int threads = 4;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();
        String key = "refund-conc-" + System.nanoTime();

        CompletableFuture<?>[] futures = new CompletableFuture[threads];
        for (int i = 0; i < threads; i++) {
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    start.await();
                    billingService.refundInvoice(billingUser, inv.getId(),
                            amount, "concurrent", key, "127.0.0.1", "ws");
                    ok.incrementAndGet();
                } catch (Throwable t) {
                    fail.incrementAndGet();
                }
            }, pool);
        }
        start.countDown();
        CompletableFuture.allOf(futures).join();
        pool.shutdown();

        assertEquals(1, ok.get(), "exactly one refund must succeed for a shared idempotency key");
        assertEquals(threads - 1, fail.get(), "all other attempts must fail");
    }
}
