package com.rescuehub.service;

import com.rescuehub.entity.SampledVisit;
import com.rescuehub.entity.SamplingRun;
import com.rescuehub.entity.User;
import com.rescuehub.entity.Visit;
import com.rescuehub.enums.VisitStatus;
import com.rescuehub.exception.BusinessRuleException;
import com.rescuehub.exception.ForbiddenException;
import com.rescuehub.exception.NotFoundException;
import com.rescuehub.enums.Role;
import com.rescuehub.repository.SampledVisitRepository;
import com.rescuehub.repository.SamplingRunRepository;
import com.rescuehub.repository.VisitRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;

@Service
public class SamplingService {

    private final SamplingRunRepository runRepo;
    private final SampledVisitRepository sampledRepo;
    private final VisitRepository visitRepo;
    private final AuditService auditService;

    public SamplingService(SamplingRunRepository runRepo, SampledVisitRepository sampledRepo,
                           VisitRepository visitRepo, AuditService auditService) {
        this.runRepo = runRepo;
        this.sampledRepo = sampledRepo;
        this.visitRepo = visitRepo;
        this.auditService = auditService;
    }

    @Transactional
    public SamplingRun createRun(User actor, String period, int percentage, String ip, String workstationId) {
        if (actor == null) throw new ForbiddenException("Authentication required");
        if (actor.getRole() != Role.QUALITY && actor.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Role not permitted to create sampling runs");
        }
        if (period == null || period.isBlank()) {
            throw new BusinessRuleException("period is required (e.g., 2026-04)");
        }
        if (percentage <= 0 || percentage > 100) {
            throw new BusinessRuleException("percentage must be between 1 and 100");
        }
        if (runRepo.existsByOrganizationIdAndPeriod(actor.getOrganizationId(), period)) {
            throw new BusinessRuleException("Sampling run already exists for period: " + period);
        }

        String seed = UUID.randomUUID().toString();

        SamplingRun run = new SamplingRun();
        run.setOrganizationId(actor.getOrganizationId());
        run.setPeriod(period);
        run.setSeed(seed);
        run.setPercentage(percentage);
        run = runRepo.save(run);

        // Select visits deterministically
        List<Visit> closedVisits = visitRepo.findByOrganizationIdAndStatus(
                actor.getOrganizationId(), VisitStatus.CLOSED);

        for (Visit v : closedVisits) {
            if (selectVisit(v.getId(), seed, percentage)) {
                SampledVisit sv = new SampledVisit();
                sv.setSamplingRunId(run.getId());
                sv.setVisitId(v.getId());
                sv.setSelectionReason("hash(visitId+seed) % 100 < " + percentage);
                sampledRepo.save(sv);
            }
        }

        auditService.log(actor.getId(), actor.getUsername(), "SAMPLING_RUN_CREATED",
                "SamplingRun", String.valueOf(run.getId()), actor.getOrganizationId(), ip, workstationId,
                null, "{\"period\":\"" + period + "\",\"percentage\":" + percentage + "}");

        return run;
    }

    /**
     * Deterministic selection: hash(visitId + seed) % 100 < percentage
     */
    public static boolean selectVisit(Long visitId, String seed, int percentage) {
        try {
            String input = visitId + seed;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            // Use first 4 bytes as int
            int val = ((hash[0] & 0xFF) << 24) | ((hash[1] & 0xFF) << 16) | ((hash[2] & 0xFF) << 8) | (hash[3] & 0xFF);
            return Math.abs(val % 100) < percentage;
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public Page<SamplingRun> list(User actor, Pageable pageable) {
        if (actor == null) throw new ForbiddenException("Authentication required");
        return runRepo.findByOrganizationId(actor.getOrganizationId(), pageable);
    }

    @Transactional(readOnly = true)
    public List<SampledVisit> getSampledVisits(User actor, Long runId) {
        if (actor == null) throw new ForbiddenException("Authentication required");
        SamplingRun run = runRepo.findById(runId)
                .orElseThrow(() -> new NotFoundException("Sampling run not found: " + runId));
        if (!run.getOrganizationId().equals(actor.getOrganizationId())) {
            throw new ForbiddenException("Sampling run belongs to a different organization");
        }
        return sampledRepo.findBySamplingRunId(runId);
    }
}
