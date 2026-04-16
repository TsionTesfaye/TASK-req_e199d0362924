package com.rescuehub.service;

import com.rescuehub.entity.IncidentReport;
import com.rescuehub.entity.User;
import com.rescuehub.enums.IncidentStatus;
import com.rescuehub.enums.Role;
import com.rescuehub.exception.BusinessRuleException;
import com.rescuehub.exception.NotFoundException;
import com.rescuehub.repository.IncidentReportRepository;
import com.rescuehub.security.RoleGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class IncidentService {

    private final IncidentReportRepository incidentRepo;
    private final CryptoService cryptoService;
    private final IdempotencyService idempotencyService;
    private final AuditService auditService;
    private final RoleGuard roleGuard;
    private final com.rescuehub.repository.DuplicateFingerprintRepository dupRepo;

    public IncidentService(IncidentReportRepository incidentRepo, CryptoService cryptoService,
                           IdempotencyService idempotencyService, AuditService auditService,
                           RoleGuard roleGuard,
                           com.rescuehub.repository.DuplicateFingerprintRepository dupRepo) {
        this.incidentRepo = incidentRepo;
        this.cryptoService = cryptoService;
        this.idempotencyService = idempotencyService;
        this.auditService = auditService;
        this.roleGuard = roleGuard;
        this.dupRepo = dupRepo;
    }

    @Transactional
    public IncidentReport submit(User actor, String idempotencyKey, String category, String description,
                                  String approximateLocationText, String neighborhood,
                                  String nearestCrossStreets, String exactLocation,
                                  boolean isAnonymous, boolean involvesMinor, boolean isProtectedCase,
                                  String subjectAgeGroup, String ip, String workstationId) {
        String cached = idempotencyService.checkAndReserve(
                actor.getOrganizationId(), actor.getId(), idempotencyKey, description);
        if (cached != null) {
            throw new BusinessRuleException("Duplicate incident submission: " + idempotencyKey);
        }

        IncidentReport incident = new IncidentReport();
        incident.setOrganizationId(actor.getOrganizationId());
        incident.setSubmittedByUserId(isAnonymous ? null : actor.getId());
        incident.setAnonymous(isAnonymous);
        incident.setCategory(category);
        incident.setDescription(description);
        incident.setApproximateLocationText(approximateLocationText);
        incident.setNeighborhood(neighborhood);
        incident.setNearestCrossStreets(nearestCrossStreets);
        incident.setInvolvesMinor(involvesMinor);
        incident.setProtectedCase(isProtectedCase);
        incident.setSubjectAgeGroup(subjectAgeGroup);
        incident.setStatus(IncidentStatus.SUBMITTED);

        // Encrypt exact location if protected/minor
        if (exactLocation != null && !exactLocation.isBlank() && (involvesMinor || isProtectedCase)) {
            CryptoService.EncryptResult enc = cryptoService.encrypt(exactLocation);
            incident.setExactLocationCiphertext(enc.ciphertext());
            incident.setExactLocationIv(enc.iv());
        }

        incident = incidentRepo.save(incident);

        // Deterministic duplicate fingerprint: category + neighborhood + nearest cross streets (normalized)
        String fp = CryptoService.sha256Hex(
                (category == null ? "" : category.toLowerCase()) + "|"
                + (neighborhood == null ? "" : neighborhood.toLowerCase()) + "|"
                + (nearestCrossStreets == null ? "" : nearestCrossStreets.toLowerCase()));
        boolean isLikelyDup = dupRepo.findByOrganizationIdAndFingerprintTypeAndFingerprintValue(
                actor.getOrganizationId(), "INCIDENT_LOCATION_CAT", fp).isPresent();
        com.rescuehub.entity.DuplicateFingerprint df = new com.rescuehub.entity.DuplicateFingerprint();
        df.setOrganizationId(actor.getOrganizationId());
        df.setFingerprintType("INCIDENT_LOCATION_CAT");
        df.setFingerprintValue(fp);
        df.setObjectType("IncidentReport");
        df.setObjectId(incident.getId());
        dupRepo.save(df);

        auditService.log(actor.getId(), actor.getUsername(),
                isLikelyDup ? "INCIDENT_SUBMIT_DUPLICATE_WARN" : "INCIDENT_SUBMIT",
                "IncidentReport", String.valueOf(incident.getId()), actor.getOrganizationId(), ip, workstationId,
                null, "{\"category\":\"" + category + "\",\"possibleDuplicate\":" + isLikelyDup + "}");

        idempotencyService.complete(actor.getOrganizationId(), idempotencyKey,
                "{\"incidentId\":" + incident.getId() + ",\"possibleDuplicate\":" + isLikelyDup + "}");
        return incident;
    }

    /**
     * Reclassify an incident's protected-case / minor status. ADMIN/MODERATOR/QUALITY only.
     * Always audited so prior visibility changes are reconstructable.
     *
     * Retroactive encryption:
     *   If the incident transitions into protected/minor AND a plaintext exactLocation
     *   is supplied (either as the optional `plaintextExactLocation` parameter, or as an
     *   existing unencrypted value carried on the entity), the plaintext is encrypted
     *   in this same transaction and a dedicated INCIDENT_RECLASSIFY_ENCRYPT audit row
     *   is written. If the incident is already encrypted (ciphertext != null), the
     *   encryption step is skipped to avoid double-encryption. Encryption failure
     *   rolls back the entire reclassification (no partial state).
     */
    @Transactional
    public IncidentReport reclassify(User actor, Long incidentId,
                                      boolean isProtectedCase, boolean involvesMinor,
                                      String plaintextExactLocation, String reason,
                                      String ip, String workstationId) {
        roleGuard.require(actor, Role.ADMIN, Role.MODERATOR, Role.QUALITY);
        IncidentReport incident = incidentRepo.findByOrganizationIdAndId(actor.getOrganizationId(), incidentId)
                .orElseThrow(() -> new NotFoundException("Incident not found: " + incidentId));

        String before = "{\"isProtectedCase\":" + incident.isProtectedCase()
                + ",\"involvesMinor\":" + incident.isInvolvesMinor()
                + ",\"hadCiphertext\":" + (incident.getExactLocationCiphertext() != null) + "}";
        incident.setProtectedCase(isProtectedCase);
        incident.setInvolvesMinor(involvesMinor);

        boolean becomesSensitive = isProtectedCase || involvesMinor;
        boolean encryptedNow = false;
        if (becomesSensitive
                && incident.getExactLocationCiphertext() == null
                && plaintextExactLocation != null
                && !plaintextExactLocation.isBlank()) {
            // Encrypt (may throw; transaction will roll back the full reclassification)
            CryptoService.EncryptResult enc = cryptoService.encrypt(plaintextExactLocation);
            incident.setExactLocationCiphertext(enc.ciphertext());
            incident.setExactLocationIv(enc.iv());
            encryptedNow = true;
        }

        incident = incidentRepo.save(incident);

        auditService.log(actor.getId(), actor.getUsername(), "INCIDENT_RECLASSIFY",
                "IncidentReport", String.valueOf(incidentId), actor.getOrganizationId(), ip, workstationId,
                before, "{\"isProtectedCase\":" + isProtectedCase + ",\"involvesMinor\":" + involvesMinor
                        + ",\"encryptedNow\":" + encryptedNow
                        + ",\"reason\":\"" + (reason == null ? "" : reason.replace("\"", "'")) + "\"}");

        if (encryptedNow) {
            auditService.log(actor.getId(), actor.getUsername(), "INCIDENT_RECLASSIFY_ENCRYPT",
                    "IncidentReport", String.valueOf(incidentId), actor.getOrganizationId(), ip, workstationId,
                    null, "{\"field\":\"exactLocation\"}");
        }
        return incident;
    }

    @Transactional
    public IncidentReport moderate(User actor, Long incidentId, IncidentStatus newStatus,
                                    String ip, String workstationId) {
        roleGuard.require(actor, Role.MODERATOR, Role.ADMIN);
        IncidentReport incident = getById(actor, incidentId);
        String before = "{\"status\":\"" + incident.getStatus() + "\"}";
        incident.setStatus(newStatus);
        incident = incidentRepo.save(incident);
        auditService.log(actor.getId(), actor.getUsername(), "INCIDENT_MODERATE",
                "IncidentReport", String.valueOf(incidentId), actor.getOrganizationId(), ip, workstationId,
                before, "{\"status\":\"" + newStatus + "\"}");
        return incident;
    }

    @Transactional(readOnly = true)
    public IncidentReport getById(User actor, Long id) {
        IncidentReport r = incidentRepo.findByOrganizationIdAndId(actor.getOrganizationId(), id)
                .orElseThrow(() -> new NotFoundException("Incident not found: " + id));
        // mask exact location for non-privileged users when protected/minor
        if ((r.isProtectedCase() || r.isInvolvesMinor()) &&
                !roleGuard.hasRole(actor, Role.ADMIN, Role.MODERATOR, Role.QUALITY)) {
            r.setExactLocationCiphertext(null);
            r.setExactLocationIv(null);
        }
        return r;
    }

    @Transactional(readOnly = true)
    public Page<IncidentReport> list(User actor, Pageable pageable) {
        return incidentRepo.findByOrganizationId(actor.getOrganizationId(), pageable);
    }

    /**
     * Reveal the exact (encrypted) location of an incident. Restricted to ADMIN / MODERATOR / QUALITY
     * and always audited — protected-case and minor locations are the most sensitive field in the system.
     */
    @Transactional
    public String revealExactLocation(User actor, Long incidentId, String ip, String workstationId) {
        IncidentReport r = incidentRepo.findByOrganizationIdAndId(actor.getOrganizationId(), incidentId)
                .orElseThrow(() -> new NotFoundException("Incident not found: " + incidentId));
        if (!roleGuard.hasRole(actor, Role.ADMIN, Role.MODERATOR, Role.QUALITY)) {
            auditService.log(actor.getId(), actor.getUsername(), "INCIDENT_LOCATION_REVEAL_DENIED",
                    "IncidentReport", String.valueOf(incidentId), actor.getOrganizationId(), ip, workstationId,
                    null, "{\"reason\":\"role_not_permitted\",\"role\":\"" + actor.getRole() + "\"}");
            throw new com.rescuehub.exception.ForbiddenException("Role not permitted to reveal exact incident location");
        }
        if (r.getExactLocationCiphertext() == null) {
            auditService.log(actor.getId(), actor.getUsername(), "INCIDENT_LOCATION_REVEAL",
                    "IncidentReport", String.valueOf(incidentId), actor.getOrganizationId(), ip, workstationId,
                    null, "{\"fieldsRevealed\":[],\"hasExactLocation\":false,\"isProtectedCase\":"
                            + r.isProtectedCase() + ",\"involvesMinor\":" + r.isInvolvesMinor() + "}");
            return null;
        }
        String plain = cryptoService.decrypt(r.getExactLocationCiphertext(), r.getExactLocationIv());
        auditService.log(actor.getId(), actor.getUsername(), "INCIDENT_LOCATION_REVEAL",
                "IncidentReport", String.valueOf(incidentId), actor.getOrganizationId(), ip, workstationId,
                null, "{\"fieldsRevealed\":[\"exactLocation\"],\"isProtectedCase\":"
                        + r.isProtectedCase() + ",\"involvesMinor\":" + r.isInvolvesMinor() + "}");
        return plain;
    }
}
