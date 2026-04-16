package com.rescuehub.service;

import com.rescuehub.entity.*;
import com.rescuehub.enums.*;
import com.rescuehub.exception.BusinessRuleException;
import com.rescuehub.exception.ConflictException;
import com.rescuehub.exception.NotFoundException;
import com.rescuehub.repository.*;
import com.rescuehub.security.RoleGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class BillingService {

    private static final BigDecimal DISCOUNT_CAP = new BigDecimal("200.00");

    private final InvoiceRepository invoiceRepo;
    private final VisitChargeRepository chargeRepo;
    private final BillingRuleRepository ruleRepo;
    private final InvoiceTenderRepository tenderRepo;
    private final PaymentRepository paymentRepo;
    private final RefundRequestRepository refundRepo;
    private final LedgerEntryRepository ledgerRepo;
    private final DailyCloseRepository dailyCloseRepo;
    private final AuditService auditService;
    private final IdempotencyService idempotencyService;
    private final RoleGuard roleGuard;

    public BillingService(InvoiceRepository invoiceRepo, VisitChargeRepository chargeRepo,
                          BillingRuleRepository ruleRepo, InvoiceTenderRepository tenderRepo,
                          PaymentRepository paymentRepo, RefundRequestRepository refundRepo,
                          LedgerEntryRepository ledgerRepo, DailyCloseRepository dailyCloseRepo,
                          AuditService auditService, IdempotencyService idempotencyService,
                          RoleGuard roleGuard) {
        this.invoiceRepo = invoiceRepo;
        this.chargeRepo = chargeRepo;
        this.ruleRepo = ruleRepo;
        this.tenderRepo = tenderRepo;
        this.paymentRepo = paymentRepo;
        this.refundRepo = refundRepo;
        this.ledgerRepo = ledgerRepo;
        this.dailyCloseRepo = dailyCloseRepo;
        this.auditService = auditService;
        this.idempotencyService = idempotencyService;
        this.roleGuard = roleGuard;
    }

    @Transactional
    public Invoice generateInvoiceForVisit(User actor, Visit visit, String ip, String workstationId) {
        if (invoiceRepo.existsByVisitId(visit.getId())) {
            return invoiceRepo.findByVisitId(visit.getId()).get();
        }

        List<VisitCharge> charges = chargeRepo.findByVisitId(visit.getId());

        // If no charges, create default office visit charge
        if (charges.isEmpty()) {
            List<BillingRule> serviceRules = ruleRepo.findByOrganizationIdAndRuleTypeAndIsActiveOrderByPriorityAsc(
                    actor.getOrganizationId(), BillingRuleType.SERVICE, true);
            for (BillingRule rule : serviceRules) {
                if ("OFFICE_VISIT".equals(rule.getCode())) {
                    VisitCharge charge = new VisitCharge();
                    charge.setVisitId(visit.getId());
                    charge.setServiceCode(rule.getCode());
                    charge.setDescription(rule.getName());
                    charge.setPricingSourceType("RULE");
                    charge.setUnitPrice(rule.getAmount());
                    charge.setQuantity(1);
                    charge.setLineTotal(rule.getAmount());
                    charge.setTaxable(true);
                    chargeRepo.save(charge);
                    charges = chargeRepo.findByVisitId(visit.getId());
                    break;
                }
            }
        }

        // Step 1: sum service charges (subtotal)
        BigDecimal subtotal = charges.stream()
                .map(VisitCharge::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Step 1.5: apply package rules — replace matched service bundles with package price
        List<BillingRule> packageRules = ruleRepo.findByOrganizationIdAndRuleTypeAndIsActiveOrderByPriorityAsc(
                actor.getOrganizationId(), BillingRuleType.PACKAGE, true);
        java.util.Set<String> chargeCodes = charges.stream()
                .map(VisitCharge::getServiceCode)
                .collect(java.util.stream.Collectors.toSet());
        for (BillingRule pr : packageRules) {
            if (pr.getAmount() == null || pr.getPackageDefinitionJson() == null) continue;
            List<String> packageCodes = parsePackageCodes(pr.getPackageDefinitionJson());
            if (!packageCodes.isEmpty() && chargeCodes.containsAll(packageCodes)) {
                BigDecimal bundledLineTotal = charges.stream()
                        .filter(c -> packageCodes.contains(c.getServiceCode()))
                        .map(VisitCharge::getLineTotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                subtotal = subtotal.subtract(bundledLineTotal).add(pr.getAmount());
                break; // apply first matching package rule only
            }
        }

        // Step 2: apply discounts — capped at $200
        List<BillingRule> discountRules = ruleRepo.findByOrganizationIdAndRuleTypeAndIsActiveOrderByPriorityAsc(
                actor.getOrganizationId(), BillingRuleType.DISCOUNT, true);
        BigDecimal totalDiscount = BigDecimal.ZERO;
        for (BillingRule dr : discountRules) {
            BigDecimal disc = BigDecimal.ZERO;
            if (dr.getPercentage() != null) {
                disc = subtotal.multiply(dr.getPercentage()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            } else if (dr.getAmount() != null) {
                disc = dr.getAmount();
            }
            totalDiscount = totalDiscount.add(disc);
        }
        // Cap discount
        if (totalDiscount.compareTo(DISCOUNT_CAP) > 0) {
            totalDiscount = DISCOUNT_CAP;
        }
        BigDecimal afterDiscount = subtotal.subtract(totalDiscount).max(BigDecimal.ZERO);

        // Step 3: apply tax
        List<BillingRule> taxRules = ruleRepo.findByOrganizationIdAndRuleTypeAndIsActiveOrderByPriorityAsc(
                actor.getOrganizationId(), BillingRuleType.TAX, true);
        BigDecimal taxableAmount = charges.stream()
                .filter(VisitCharge::isTaxable)
                .map(VisitCharge::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .subtract(totalDiscount).max(BigDecimal.ZERO);
        BigDecimal totalTax = BigDecimal.ZERO;
        for (BillingRule tr : taxRules) {
            if (tr.getTaxRate() != null) {
                totalTax = totalTax.add(taxableAmount.multiply(tr.getTaxRate())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            }
        }

        BigDecimal total = afterDiscount.add(totalTax);

        Invoice invoice = new Invoice();
        invoice.setOrganizationId(actor.getOrganizationId());
        invoice.setPatientId(visit.getPatientId());
        invoice.setVisitId(visit.getId());
        invoice.setInvoiceNumber("INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        invoice.setStatus(InvoiceStatus.OPEN);
        invoice.setSubtotalAmount(subtotal);
        invoice.setDiscountAmount(totalDiscount);
        invoice.setTaxAmount(totalTax);
        invoice.setTotalAmount(total);
        invoice.setOutstandingAmount(total);
        invoice.setGeneratedAt(Instant.now());
        invoice = invoiceRepo.save(invoice);

        // Ledger entries
        appendLedger(actor.getOrganizationId(), invoice.getId(), null, LedgerEntryType.INVOICE_GENERATED, total);
        if (totalDiscount.compareTo(BigDecimal.ZERO) > 0)
            appendLedger(actor.getOrganizationId(), invoice.getId(), null, LedgerEntryType.DISCOUNT_RECORDED, totalDiscount);
        if (totalTax.compareTo(BigDecimal.ZERO) > 0)
            appendLedger(actor.getOrganizationId(), invoice.getId(), null, LedgerEntryType.TAX_RECORDED, totalTax);

        auditService.log(actor.getId(), actor.getUsername(), "INVOICE_GENERATED",
                "Invoice", String.valueOf(invoice.getId()), actor.getOrganizationId(), ip, workstationId,
                null, "{\"invoiceNumber\":\"" + invoice.getInvoiceNumber() + "\",\"total\":" + total + "}");

        return invoice;
    }

    @Transactional
    public Payment recordPayment(User actor, Long invoiceId, TenderType tenderType, BigDecimal amount,
                                  String externalReference, String ip, String workstationId) {
        roleGuard.require(actor, Role.BILLING, Role.ADMIN);
        Invoice invoice = invoiceRepo.findByOrganizationIdAndId(actor.getOrganizationId(), invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found"));

        if (invoice.getStatus() == InvoiceStatus.VOIDED)
            throw new BusinessRuleException("Cannot record payment on voided invoice");
        if (invoice.getOutstandingAmount().compareTo(BigDecimal.ZERO) <= 0)
            throw new BusinessRuleException("Invoice already fully paid");

        Payment payment = new Payment();
        payment.setOrganizationId(actor.getOrganizationId());
        payment.setInvoiceId(invoiceId);
        payment.setTenderType(tenderType);
        payment.setAmount(amount);
        payment.setOccurredAt(Instant.now());
        payment.setExternalReference(externalReference);
        payment = paymentRepo.save(payment);

        InvoiceTender tender = new InvoiceTender();
        tender.setInvoiceId(invoiceId);
        tender.setTenderType(tenderType);
        tender.setAmount(amount);
        tender.setStatus(TenderStatus.RECORDED);
        tender.setExternalReference(externalReference);
        tenderRepo.save(tender);

        BigDecimal newOutstanding = invoice.getOutstandingAmount().subtract(amount).max(BigDecimal.ZERO);
        invoice.setOutstandingAmount(newOutstanding);
        invoice.setStatus(newOutstanding.compareTo(BigDecimal.ZERO) == 0 ? InvoiceStatus.PAID : InvoiceStatus.PARTIALLY_PAID);
        invoiceRepo.save(invoice);

        appendLedger(actor.getOrganizationId(), invoiceId, null, LedgerEntryType.PAYMENT_RECORDED, amount);

        auditService.log(actor.getId(), actor.getUsername(), "PAYMENT_RECORDED",
                "Invoice", String.valueOf(invoiceId), actor.getOrganizationId(), ip, workstationId,
                null, "{\"amount\":" + amount + ",\"type\":\"" + tenderType + "\"}");

        return payment;
    }

    @Transactional
    public Invoice voidInvoice(User actor, Long invoiceId, String idempotencyKey, String ip, String workstationId) {
        // Role check: only BILLING or ADMIN may void
        if (actor.getRole() != Role.BILLING && actor.getRole() != Role.ADMIN) {
            throw new com.rescuehub.exception.ForbiddenException("Role not permitted to void invoices");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessRuleException("Idempotency-Key header is required for void operations");
        }
        String cached = idempotencyService.checkAndReserve(
                actor.getOrganizationId(), actor.getId(), idempotencyKey, "VOID:" + invoiceId);
        if (cached != null) {
            return invoiceRepo.findByOrganizationIdAndId(actor.getOrganizationId(), invoiceId)
                    .orElseThrow(() -> new NotFoundException("Invoice not found"));
        }

        Invoice invoice = invoiceRepo.findByOrganizationIdAndId(actor.getOrganizationId(), invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found"));

        if (invoice.getStatus() == InvoiceStatus.VOIDED) {
            idempotencyService.complete(actor.getOrganizationId(), idempotencyKey,
                    "{\"invoiceId\":" + invoiceId + ",\"status\":\"VOIDED\"}");
            return invoice;
        }
        if (invoice.getStatus() == InvoiceStatus.REFUNDED
                || invoice.getStatus() == InvoiceStatus.PARTIALLY_REFUNDED) {
            throw new BusinessRuleException("Cannot void a refunded invoice");
        }

        // Policy boundary: void only before 11:00 PM server-local on the invoice's business date.
        // The cutoff is hard: if now >= businessDate 23:00, reject regardless of whether a
        // daily-close row exists. The daily-close check is an additional guard on top of this.
        java.time.LocalDate businessDate = invoice.getDailyCloseDate() != null
                ? invoice.getDailyCloseDate()
                : java.time.LocalDate.now();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime cutoff = businessDate.atTime(23, 0);
        if (!now.isBefore(cutoff)) {
            throw new BusinessRuleException("Cannot void invoice after 11:00 PM on its business date");
        }
        if (dailyCloseRepo.existsByOrganizationIdAndBusinessDate(actor.getOrganizationId(), businessDate)) {
            throw new BusinessRuleException("Cannot void invoice: daily close already exists for " + businessDate);
        }

        String before = "{\"status\":\"" + invoice.getStatus() + "\"}";
        invoice.setStatus(InvoiceStatus.VOIDED);
        invoice.setVoidedAt(Instant.now());
        invoiceRepo.save(invoice);

        appendLedger(actor.getOrganizationId(), invoiceId, null, LedgerEntryType.INVOICE_VOIDED, invoice.getTotalAmount());

        auditService.log(actor.getId(), actor.getUsername(), "INVOICE_VOIDED",
                "Invoice", String.valueOf(invoiceId), actor.getOrganizationId(), ip, workstationId,
                before, "{\"status\":\"VOIDED\"}");

        idempotencyService.complete(actor.getOrganizationId(), idempotencyKey,
                "{\"invoiceId\":" + invoiceId + ",\"status\":\"VOIDED\"}");
        return invoice;
    }

    @Transactional
    public RefundRequest refundInvoice(User actor, Long invoiceId, BigDecimal amount, String reason,
                                        String idempotencyKey, String ip, String workstationId) {
        // Role check: only BILLING or ADMIN may refund
        if (actor.getRole() != Role.BILLING && actor.getRole() != Role.ADMIN) {
            throw new com.rescuehub.exception.ForbiddenException("Role not permitted to refund invoices");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessRuleException("Idempotency-Key header is required for refund operations");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleException("Refund amount must be positive");
        }
        // Monetary precision: reject more than 2 decimal places
        if (amount.scale() > 2) {
            throw new BusinessRuleException("Refund amount precision exceeds 2 decimal places");
        }
        amount = amount.setScale(2, RoundingMode.HALF_UP);

        String cached = idempotencyService.checkAndReserve(
                actor.getOrganizationId(), actor.getId(), idempotencyKey, "REFUND:" + invoiceId + ":" + amount);
        if (cached != null) {
            throw new BusinessRuleException("Duplicate refund submission: " + idempotencyKey);
        }

        Invoice invoice = invoiceRepo.findByOrganizationIdAndId(actor.getOrganizationId(), invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found"));
        if (invoice.getStatus() == InvoiceStatus.VOIDED) {
            throw new BusinessRuleException("Cannot refund a voided invoice");
        }

        // 30-day window
        Instant thirtyDaysAgo = Instant.now().minusSeconds(30L * 24 * 3600);
        if (invoice.getGeneratedAt().isBefore(thirtyDaysAgo)) {
            throw new BusinessRuleException("Refund window of 30 days has expired");
        }

        // check refundable balance
        BigDecimal alreadyRefunded = refundRepo.sumExecutedRefundsByInvoiceId(invoiceId, RefundStatus.EXECUTED);
        BigDecimal refundable = invoice.getTotalAmount().subtract(alreadyRefunded);
        if (amount.compareTo(refundable) > 0) {
            throw new BusinessRuleException("Refund amount " + amount + " exceeds refundable balance " + refundable);
        }

        RefundRequest rr = new RefundRequest();
        rr.setInvoiceId(invoiceId);
        rr.setRequestedByUserId(actor.getId());
        rr.setRefundAmount(amount);
        rr.setRefundReason(reason);
        rr.setStatus(RefundStatus.EXECUTED);
        rr.setApprovedAt(Instant.now());
        rr.setExecutedAt(Instant.now());
        rr = refundRepo.save(rr);

        BigDecimal totalRefunded = alreadyRefunded.add(amount);
        invoice.setStatus(totalRefunded.compareTo(invoice.getTotalAmount()) >= 0
                ? InvoiceStatus.REFUNDED : InvoiceStatus.PARTIALLY_REFUNDED);
        invoice.setRefundedAt(Instant.now());
        invoiceRepo.save(invoice);

        appendLedger(actor.getOrganizationId(), invoiceId, rr.getId(), LedgerEntryType.REFUND_EXECUTED, amount);

        auditService.log(actor.getId(), actor.getUsername(), "INVOICE_REFUNDED",
                "Invoice", String.valueOf(invoiceId), actor.getOrganizationId(), ip, workstationId,
                null, "{\"amount\":" + amount + "}");

        idempotencyService.complete(actor.getOrganizationId(), idempotencyKey,
                "{\"refundId\":" + rr.getId() + ",\"amount\":" + amount + "}");
        return rr;
    }

    @Transactional(readOnly = true)
    public Page<Invoice> listInvoices(User actor, Pageable pageable) {
        roleGuard.require(actor, Role.BILLING, Role.ADMIN);
        return invoiceRepo.findByOrganizationId(actor.getOrganizationId(), pageable);
    }

    @Transactional(readOnly = true)
    public Invoice getInvoice(User actor, Long id) {
        roleGuard.require(actor, Role.BILLING, Role.ADMIN);
        return invoiceRepo.findByOrganizationIdAndId(actor.getOrganizationId(), id)
                .orElseThrow(() -> new NotFoundException("Invoice not found"));
    }

    @Transactional(readOnly = true)
    public java.util.List<Payment> listPayments(User actor, Long invoiceId) {
        roleGuard.require(actor, Role.BILLING, Role.ADMIN);
        // Object-level scope: invoice must belong to actor's org
        getInvoice(actor, invoiceId);
        return paymentRepo.findByInvoiceId(invoiceId);
    }

    /** Parse service codes from a JSON array string, e.g. ["CODE_A","CODE_B"]. */
    private List<String> parsePackageCodes(String json) {
        if (json == null || json.isBlank()) return List.of();
        List<String> codes = new java.util.ArrayList<>();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"([^\"]+)\"").matcher(json);
        while (m.find()) codes.add(m.group(1));
        return codes;
    }

    private void appendLedger(Long orgId, Long invoiceId, Long refundId, LedgerEntryType type, BigDecimal amount) {
        LedgerEntry entry = new LedgerEntry();
        entry.setOrganizationId(orgId);
        entry.setInvoiceId(invoiceId);
        entry.setRefundRequestId(refundId);
        entry.setEntryType(type);
        entry.setAmount(amount);
        entry.setOccurredAt(Instant.now());
        ledgerRepo.save(entry);
    }
}
