package com.rescuehub.service;

import com.rescuehub.entity.User;
import com.rescuehub.entity.Visit;
import com.rescuehub.enums.Role;
import com.rescuehub.enums.VisitStatus;
import com.rescuehub.exception.BusinessRuleException;
import com.rescuehub.exception.NotFoundException;
import com.rescuehub.repository.PatientRepository;
import com.rescuehub.repository.VisitRepository;
import com.rescuehub.security.RoleGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class VisitService {

    private final VisitRepository visitRepo;
    private final PatientRepository patientRepo;
    private final QualityRulesService qualityService;
    private final BillingService billingService;
    private final IdempotencyService idempotencyService;
    private final AuditService auditService;
    private final RoleGuard roleGuard;

    public VisitService(VisitRepository visitRepo, PatientRepository patientRepo,
                        QualityRulesService qualityService,
                        BillingService billingService, IdempotencyService idempotencyService,
                        AuditService auditService, RoleGuard roleGuard) {
        this.visitRepo = visitRepo;
        this.patientRepo = patientRepo;
        this.qualityService = qualityService;
        this.billingService = billingService;
        this.idempotencyService = idempotencyService;
        this.auditService = auditService;
        this.roleGuard = roleGuard;
    }

    @Transactional
    public Visit open(User actor, Long patientId, Long appointmentId, String chiefComplaint,
                      String idempotencyKey, String ip, String workstationId) {
        roleGuard.require(actor, Role.CLINICIAN, Role.ADMIN);
        // Validate patient exists in actor's organization (object integrity check)
        patientRepo.findByOrganizationIdAndId(actor.getOrganizationId(), patientId)
                .orElseThrow(() -> new NotFoundException("Patient not found: " + patientId));
        // Idempotency for visit creation: replay returns cached visit id
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String cached = idempotencyService.checkAndReserve(
                    actor.getOrganizationId(), actor.getId(), idempotencyKey,
                    "VISIT_OPEN:" + patientId);
            if (cached != null) {
                Long cachedId = parseVisitIdFromSnapshot(cached);
                if (cachedId != null) {
                    return visitRepo.findByOrganizationIdAndId(actor.getOrganizationId(), cachedId)
                            .orElseThrow(() -> new NotFoundException("Visit not found"));
                }
            }
        }

        Visit visit = new Visit();
        visit.setOrganizationId(actor.getOrganizationId());
        visit.setPatientId(patientId);
        visit.setAppointmentId(appointmentId);
        visit.setCreatedByUserId(actor.getId());
        visit.setOpenedAt(Instant.now());
        visit.setStatus(VisitStatus.OPEN);
        visit.setChiefComplaint(chiefComplaint);
        visit = visitRepo.save(visit);

        // Run QC rules immediately
        qualityService.runRulesForVisit(visit);
        visit = visitRepo.save(visit);

        auditService.log(actor.getId(), actor.getUsername(), "VISIT_OPEN",
                "Visit", String.valueOf(visit.getId()), actor.getOrganizationId(), ip, workstationId,
                null, "{\"patientId\":" + patientId + "}");

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.complete(actor.getOrganizationId(), idempotencyKey,
                    "{\"visitId\":" + visit.getId() + "}");
        }
        return visit;
    }

    private Long parseVisitIdFromSnapshot(String snapshot) {
        if (snapshot == null) return null;
        int i = snapshot.indexOf("\"visitId\":");
        if (i < 0) return null;
        int start = i + 10;
        int end = start;
        while (end < snapshot.length() && Character.isDigit(snapshot.charAt(end))) end++;
        if (end == start) return null;
        try { return Long.parseLong(snapshot.substring(start, end)); } catch (Exception e) { return null; }
    }

    @Transactional
    public Visit updateSummary(User actor, Long visitId, String summaryText, String diagnosisText,
                                String ip, String workstationId) {
        roleGuard.require(actor, Role.CLINICIAN, Role.ADMIN);
        Visit visit = getById(actor, visitId);
        if (visit.getStatus() == VisitStatus.CLOSED || visit.getStatus() == VisitStatus.ARCHIVED) {
            throw new BusinessRuleException("Cannot update a closed or archived visit");
        }
        String before = "{\"summaryText\":\"" + (visit.getSummaryText() != null ? "..." : "null") + "\"}";
        visit.setSummaryText(summaryText);
        visit.setDiagnosisText(diagnosisText);
        visit = visitRepo.save(visit);
        auditService.log(actor.getId(), actor.getUsername(), "VISIT_UPDATE",
                "Visit", String.valueOf(visitId), actor.getOrganizationId(), ip, workstationId, before, null);
        return visit;
    }

    @Transactional
    public Visit closeVisit(User actor, Long visitId, String idempotencyKey, String ip, String workstationId) {
        roleGuard.require(actor, Role.CLINICIAN, Role.ADMIN);
        // Idempotency check
        String cached = idempotencyService.checkAndReserve(
                actor.getOrganizationId(), actor.getId(), idempotencyKey, visitId.toString());
        if (cached != null) {
            return visitRepo.findByOrganizationIdAndId(actor.getOrganizationId(), visitId)
                    .orElseThrow(() -> new NotFoundException("Visit not found"));
        }

        Visit visit = getById(actor, visitId);
        if (visit.getStatus() == VisitStatus.CLOSED) {
            auditService.log(actor.getId(), actor.getUsername(), "VISIT_CLOSE_DUPLICATE_NOOP",
                    "Visit", String.valueOf(visitId), actor.getOrganizationId(), ip, workstationId,
                    null, "{\"idempotencyKey\":\"" + idempotencyKey + "\"}");
            idempotencyService.complete(actor.getOrganizationId(), idempotencyKey, "{\"status\":\"CLOSED\"}");
            return visit;
        }
        if (visit.getStatus() == VisitStatus.ARCHIVED) {
            throw new BusinessRuleException("Cannot close an archived visit");
        }
        if (visit.getStatus() != VisitStatus.OPEN && visit.getStatus() != VisitStatus.READY_FOR_CLOSE) {
            throw new BusinessRuleException("Visit cannot be closed from status: " + visit.getStatus());
        }

        // Check QC blocking
        if (qualityService.hasUnresolvedBlocking(visitId)) {
            throw new BusinessRuleException("Visit has unresolved blocking QC issues. Obtain quality override first.");
        }

        visit.setStatus(VisitStatus.CLOSED);
        visit.setClosedAt(Instant.now());
        visit = visitRepo.save(visit);

        // Generate invoice atomically
        billingService.generateInvoiceForVisit(actor, visit, ip, workstationId);

        auditService.log(actor.getId(), actor.getUsername(), "VISIT_CLOSE",
                "Visit", String.valueOf(visitId), actor.getOrganizationId(), ip, workstationId,
                "{\"status\":\"OPEN\"}", "{\"status\":\"CLOSED\"}");

        idempotencyService.complete(actor.getOrganizationId(), idempotencyKey, "{\"status\":\"CLOSED\",\"visitId\":" + visitId + "}");
        return visit;
    }

    @Transactional(readOnly = true)
    public Visit getById(User actor, Long visitId) {
        roleGuard.require(actor, Role.FRONT_DESK, Role.CLINICIAN, Role.ADMIN);
        return visitRepo.findByOrganizationIdAndId(actor.getOrganizationId(), visitId)
                .orElseThrow(() -> new NotFoundException("Visit not found: " + visitId));
    }

    @Transactional(readOnly = true)
    public Page<Visit> list(User actor, Long patientId, Pageable pageable) {
        roleGuard.require(actor, Role.FRONT_DESK, Role.CLINICIAN, Role.ADMIN);
        if (patientId != null) {
            return visitRepo.findByOrganizationIdAndPatientId(actor.getOrganizationId(), patientId, pageable);
        }
        return visitRepo.findByOrganizationId(actor.getOrganizationId(), pageable);
    }
}
