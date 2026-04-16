package com.rescuehub.controller;

import com.rescuehub.entity.User;
import com.rescuehub.entity.Visit;
import com.rescuehub.security.SecurityUtils;
import com.rescuehub.service.VisitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/visits")
public class VisitController {

    private final VisitService visitService;

    public VisitController(VisitService visitService) {
        this.visitService = visitService;
    }

    record OpenRequest(@NotNull Long patientId, Long appointmentId, String chiefComplaint) {}
    record UpdateRequest(String summaryText, String diagnosisText) {}

    @PostMapping
    public Map<String, Object> open(@Valid @RequestBody OpenRequest req,
                                     @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                     HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        Visit visit = visitService.open(actor, req.patientId(), req.appointmentId(), req.chiefComplaint(),
                idempotencyKey, request.getRemoteAddr(), SecurityUtils.currentWorkstationId());
        return ApiResponse.data(visit);
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     @RequestParam(required = false) Long patientId) {
        User actor = SecurityUtils.currentUser();
        Page<Visit> visits = visitService.list(actor, patientId, PageBounds.of(page, size));
        return ApiResponse.list(visits.getContent(), visits.getTotalElements(), page, size);
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Long id) {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(visitService.getById(actor, id));
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @Valid @RequestBody UpdateRequest req,
                                       HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(visitService.updateSummary(actor, id, req.summaryText(), req.diagnosisText(),
                request.getRemoteAddr(), SecurityUtils.currentWorkstationId()));
    }

    @PostMapping("/{id}/close")
    public Map<String, Object> close(@PathVariable Long id,
                                      @RequestHeader("Idempotency-Key") String idempotencyKey,
                                      HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        Visit visit = visitService.closeVisit(actor, id, idempotencyKey, request.getRemoteAddr(),
                SecurityUtils.currentWorkstationId());
        return ApiResponse.data(visit);
    }
}
