package com.rescuehub.controller;

import com.rescuehub.entity.User;
import com.rescuehub.enums.TenderType;
import com.rescuehub.security.SecurityUtils;
import com.rescuehub.service.BillingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    record PaymentRequest(@NotNull TenderType tenderType, @NotNull BigDecimal amount, String externalReference) {}
    record RefundRequest(@NotNull BigDecimal amount, @NotNull String reason) {}

    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        User actor = SecurityUtils.currentUser();
        var invoices = billingService.listInvoices(actor, PageBounds.of(page, size));
        return ApiResponse.list(invoices.getContent(), invoices.getTotalElements(), page, size);
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Long id) {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(billingService.getInvoice(actor, id));
    }

    @GetMapping("/{id}/payments")
    public Map<String, Object> listPayments(@PathVariable Long id) {
        User actor = SecurityUtils.currentUser();
        var payments = billingService.listPayments(actor, id);
        return ApiResponse.list(payments, payments.size(), 0, payments.size());
    }

    @PostMapping("/{id}/payments")
    public Map<String, Object> recordPayment(@PathVariable Long id, @Valid @RequestBody PaymentRequest req,
                                              HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(billingService.recordPayment(actor, id, req.tenderType(), req.amount(),
                req.externalReference(), request.getRemoteAddr(), SecurityUtils.currentWorkstationId()));
    }

    @PostMapping("/{id}/void")
    public Map<String, Object> voidInvoice(@PathVariable Long id,
                                            @RequestHeader("Idempotency-Key") String idempotencyKey,
                                            HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(billingService.voidInvoice(actor, id, idempotencyKey,
                request.getRemoteAddr(), SecurityUtils.currentWorkstationId()));
    }

    @PostMapping("/{id}/refunds")
    public Map<String, Object> refund(@PathVariable Long id, @Valid @RequestBody RefundRequest req,
                                       @RequestHeader("Idempotency-Key") String idempotencyKey,
                                       HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(billingService.refundInvoice(actor, id, req.amount(), req.reason(),
                idempotencyKey, request.getRemoteAddr(), SecurityUtils.currentWorkstationId()));
    }
}
