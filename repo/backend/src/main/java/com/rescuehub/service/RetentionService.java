package com.rescuehub.service;

import com.rescuehub.entity.Patient;
import com.rescuehub.entity.RetentionPolicyHold;
import com.rescuehub.repository.PatientRepository;
import com.rescuehub.repository.RetentionPolicyHoldRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class RetentionService {

    private final PatientRepository patientRepo;
    private final RetentionPolicyHoldRepository holdRepo;
    private final AuditService auditService;

    public RetentionService(PatientRepository patientRepo, RetentionPolicyHoldRepository holdRepo,
                            AuditService auditService) {
        this.patientRepo = patientRepo;
        this.holdRepo = holdRepo;
        this.auditService = auditService;
    }

    @Transactional
    public int archiveEligible(Long orgId) {
        // 7 years threshold
        Instant threshold = Instant.now().minusSeconds(7L * 365 * 24 * 3600);
        List<Patient> candidates = patientRepo.findActiveOlderThan(orgId, threshold);
        int count = 0;
        for (Patient p : candidates) {
            // Check no legal hold active
            List<RetentionPolicyHold> holds = holdRepo.findByPatientId(p.getId());
            boolean onHold = holds.stream().anyMatch(h ->
                    h.getHoldUntil() == null || h.getHoldUntil().isAfter(Instant.now()));
            if (!onHold) {
                p.setArchivedAt(Instant.now());
                patientRepo.save(p);
                auditService.log(null, "SYSTEM", "PATIENT_RETENTION_ARCHIVE",
                        "Patient", String.valueOf(p.getId()), orgId, null, null, null,
                        "{\"archivedAt\":\"" + p.getArchivedAt() + "\"}");
                count++;
            }
        }
        return count;
    }
}
