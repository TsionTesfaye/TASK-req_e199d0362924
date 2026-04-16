package com.rescuehub.service;

import com.rescuehub.entity.*;
import com.rescuehub.enums.QualityResultStatus;
import com.rescuehub.enums.QualitySeverity;
import com.rescuehub.exception.BusinessRuleException;
import com.rescuehub.exception.ForbiddenException;
import com.rescuehub.exception.NotFoundException;
import com.rescuehub.repository.*;
import com.rescuehub.security.RoleGuard;
import com.rescuehub.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Service
public class QualityRulesService {

    private final QualityRuleResultRepository resultRepo;
    private final QualityOverrideRepository overrideRepo;
    private final VisitRepository visitRepo;
    private final PatientRepository patientRepo;
    private final AuditService auditService;
    private final RoleGuard roleGuard;

    public QualityRulesService(QualityRuleResultRepository resultRepo,
                                QualityOverrideRepository overrideRepo,
                                VisitRepository visitRepo,
                                PatientRepository patientRepo,
                                AuditService auditService,
                                RoleGuard roleGuard) {
        this.resultRepo = resultRepo;
        this.overrideRepo = overrideRepo;
        this.visitRepo = visitRepo;
        this.patientRepo = patientRepo;
        this.auditService = auditService;
        this.roleGuard = roleGuard;
    }

    @Transactional
    public void runRulesForVisit(Visit visit) {
        Patient patient = patientRepo.findByOrganizationIdAndId(visit.getOrganizationId(), visit.getPatientId())
                .orElse(null);
        if (patient == null) return;

        // Rule 1: Age vs DOB cross-check — if visit says involves_minor, verify
        boolean isMinorByDob = patient.getDateOfBirth() != null &&
                Period.between(patient.getDateOfBirth(), LocalDate.now()).getYears() < 18;
        if (patient.isMinor() != isMinorByDob) {
            createResult(visit, patient, "AGE_DOB_MISMATCH", QualitySeverity.WARNING,
                    "{\"message\":\"Minor flag does not match computed age from DOB\"}");
        }

        // Rule 2: 7-day frequency anomaly — more than 3 visits in 7 days
        long recentCount = visitRepo.countRecentClosedVisits(
                visit.getOrganizationId(), visit.getPatientId(),
                Instant.now().minusSeconds(7 * 24 * 3600), com.rescuehub.enums.VisitStatus.CLOSED);
        if (recentCount >= 3) {
            createResult(visit, patient, "HIGH_VISIT_FREQUENCY", QualitySeverity.BLOCKING,
                    "{\"message\":\"Patient has " + recentCount + " visits in last 7 days\",\"count\":" + recentCount + "}");
        }

        // Rule 3: Missing required fields
        if (visit.getChiefComplaint() == null || visit.getChiefComplaint().isBlank()) {
            createResult(visit, patient, "MISSING_CHIEF_COMPLAINT", QualitySeverity.WARNING,
                    "{\"message\":\"Chief complaint is missing\"}");
        }

        // Update visit qcBlocked flag
        List<QualityRuleResult> blocking = resultRepo.findByVisitIdAndSeverityAndStatus(
                visit.getId(), QualitySeverity.BLOCKING, QualityResultStatus.OPEN);
        if (!blocking.isEmpty()) {
            visit.setQcBlocked(true);
        }
    }

    private void createResult(Visit visit, Patient patient, String ruleCode, QualitySeverity severity, String details) {
        QualityRuleResult r = new QualityRuleResult();
        r.setOrganizationId(visit.getOrganizationId());
        r.setVisitId(visit.getId());
        r.setPatientId(patient.getId());
        r.setRuleCode(ruleCode);
        r.setSeverity(severity);
        r.setStatus(QualityResultStatus.OPEN);
        r.setResultDetailsJson(details);
        resultRepo.save(r);
    }

    public boolean hasUnresolvedBlocking(Long visitId) {
        List<QualityRuleResult> blocking = resultRepo.findByVisitIdAndSeverityAndStatus(
                visitId, QualitySeverity.BLOCKING, QualityResultStatus.OPEN);
        return !blocking.isEmpty();
    }

    @Transactional(readOnly = true)
    public Page<QualityRuleResult> listResults(User actor, Pageable pageable) {
        roleGuard.require(actor, Role.QUALITY, Role.ADMIN);
        return resultRepo.findByOrganizationId(actor.getOrganizationId(), pageable);
    }

    @Transactional(readOnly = true)
    public QualityRuleResult getResult(User actor, Long id) {
        roleGuard.require(actor, Role.QUALITY, Role.ADMIN);
        return resultRepo.findByOrganizationIdAndId(actor.getOrganizationId(), id)
                .orElseThrow(() -> new NotFoundException("Quality result not found"));
    }

    @Transactional
    public QualityOverride override(User actor, Long resultId, String reasonCode, String note,
                                    String ip, String workstationId) {
        roleGuard.require(actor, Role.QUALITY, Role.ADMIN);
        if (note == null || note.isBlank()) throw new BusinessRuleException("Override note is required");

        QualityRuleResult result = resultRepo.findByOrganizationIdAndId(actor.getOrganizationId(), resultId)
                .orElseThrow(() -> new NotFoundException("Quality result not found"));

        result.setStatus(QualityResultStatus.OVERRIDDEN);
        result.setResolvedAt(Instant.now());
        resultRepo.save(result);

        QualityOverride override = new QualityOverride();
        override.setQualityRuleResultId(resultId);
        override.setOverriddenByUserId(actor.getId());
        override.setOverrideReasonCode(reasonCode);
        override.setOverrideNote(note);
        overrideRepo.save(override);

        auditService.log(actor.getId(), actor.getUsername(), "QC_OVERRIDE",
                "QualityRuleResult", String.valueOf(resultId), actor.getOrganizationId(), ip, workstationId,
                "{\"status\":\"OPEN\"}", "{\"status\":\"OVERRIDDEN\",\"reason\":\"" + reasonCode + "\"}");

        return override;
    }
}
