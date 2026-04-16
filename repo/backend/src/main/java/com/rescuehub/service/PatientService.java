package com.rescuehub.service;

import com.rescuehub.entity.DuplicateFingerprint;
import com.rescuehub.entity.Patient;
import com.rescuehub.entity.PatientIdentityVerification;
import com.rescuehub.entity.User;
import com.rescuehub.enums.Role;
import com.rescuehub.exception.BusinessRuleException;
import com.rescuehub.exception.NotFoundException;
import com.rescuehub.repository.DuplicateFingerprintRepository;
import com.rescuehub.repository.PatientIdentityVerificationRepository;
import com.rescuehub.repository.PatientRepository;
import com.rescuehub.security.RoleGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class PatientService {

    private final PatientRepository patientRepo;
    private final DuplicateFingerprintRepository dupRepo;
    private final CryptoService cryptoService;
    private final AuditService auditService;
    private final PatientIdentityVerificationRepository pivRepo;
    private final RoleGuard roleGuard;

    public PatientService(PatientRepository patientRepo, DuplicateFingerprintRepository dupRepo,
                          CryptoService cryptoService, AuditService auditService,
                          PatientIdentityVerificationRepository pivRepo, RoleGuard roleGuard) {
        this.patientRepo = patientRepo;
        this.dupRepo = dupRepo;
        this.cryptoService = cryptoService;
        this.auditService = auditService;
        this.pivRepo = pivRepo;
        this.roleGuard = roleGuard;
    }

    /**
     * Record a patient ID verification. FRONT_DESK / ADMIN only.
     * Stores ONLY documentType + last4 (per design.md §6.5 — full identifier is never persisted).
     */
    @Transactional
    public PatientIdentityVerification verifyIdentity(User actor, Long patientId,
                                                       String documentType, String documentLast4,
                                                       String note, String ip, String workstationId) {
        roleGuard.require(actor, Role.FRONT_DESK, Role.CLINICIAN, Role.ADMIN);
        if (documentType == null || documentType.isBlank()) {
            throw new BusinessRuleException("documentType is required");
        }
        if (documentLast4 == null || !documentLast4.matches("\\d{4}")) {
            throw new BusinessRuleException("documentLast4 must be exactly 4 digits");
        }
        // Object-level scope: patient must exist in actor's org
        Patient patient = getById(actor, patientId);

        PatientIdentityVerification piv = new PatientIdentityVerification();
        piv.setPatientId(patient.getId());
        piv.setVerifiedByUserId(actor.getId());
        piv.setDocumentType(documentType);
        piv.setDocumentLast4(documentLast4);
        piv.setVerifiedAt(Instant.now());
        piv.setNote(note);
        piv = pivRepo.save(piv);

        auditService.log(actor.getId(), actor.getUsername(), "PATIENT_ID_VERIFIED",
                "PatientIdentityVerification", String.valueOf(piv.getId()),
                actor.getOrganizationId(), ip, workstationId, null,
                "{\"patientId\":" + patient.getId() + ",\"documentType\":\"" + documentType
                        + "\",\"last4\":\"" + documentLast4 + "\"}");
        return piv;
    }

    @Transactional(readOnly = true)
    public java.util.List<PatientIdentityVerification> listVerifications(User actor, Long patientId) {
        // Object-level scope through getById
        Patient patient = getById(actor, patientId);
        return pivRepo.findByPatientId(patient.getId());
    }

    @Transactional
    public Patient register(User actor, String firstName, String lastName, LocalDate dob,
                            String sex, String phone, String address,
                            String emergencyContactName, String emergencyContactPhone,
                            boolean isMinor, boolean isProtectedCase, String ip, String workstationId) {
        roleGuard.require(actor, Role.FRONT_DESK, Role.CLINICIAN, Role.ADMIN);
        String fingerprint = CryptoService.sha256Hex(
                firstName.toLowerCase() + "|" + lastName.toLowerCase() + "|" + dob.toString());

        // check for existing fingerprint → warn but allow (log as duplicate detection)
        boolean dupExists = dupRepo.findByOrganizationIdAndFingerprintTypeAndFingerprintValue(
                actor.getOrganizationId(), "PATIENT_IDENTITY", fingerprint).isPresent();

        Patient patient = new Patient();
        patient.setOrganizationId(actor.getOrganizationId());
        patient.setMedicalRecordNumber("MRN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        patient.setDateOfBirth(dob);
        patient.setSex(sex);
        patient.setMinor(isMinor);
        patient.setProtectedCase(isProtectedCase);

        // encrypt name
        CryptoService.EncryptResult fn = cryptoService.encrypt(firstName);
        patient.setFirstNameCiphertext(fn.ciphertext());
        patient.setFirstNameIv(fn.iv());
        CryptoService.EncryptResult ln = cryptoService.encrypt(lastName);
        patient.setLastNameCiphertext(ln.ciphertext());
        patient.setLastNameIv(ln.iv());

        // encrypt phone
        if (phone != null && !phone.isBlank()) {
            CryptoService.EncryptResult ph = cryptoService.encrypt(phone);
            patient.setPhoneCiphertext(ph.ciphertext());
            patient.setPhoneIv(ph.iv());
            patient.setPhoneLast4(phone.length() >= 4 ? phone.substring(phone.length() - 4) : phone);
        }

        // encrypt address
        if (address != null && !address.isBlank()) {
            CryptoService.EncryptResult ad = cryptoService.encrypt(address);
            patient.setAddressCiphertext(ad.ciphertext());
            patient.setAddressIv(ad.iv());
        }

        patient.setEmergencyContactName(emergencyContactName);
        if (emergencyContactPhone != null && !emergencyContactPhone.isBlank()) {
            CryptoService.EncryptResult ec = cryptoService.encrypt(emergencyContactPhone);
            patient.setEmergencyContactPhoneCiphertext(ec.ciphertext());
            patient.setEmergencyContactPhoneIv(ec.iv());
        }

        patient = patientRepo.save(patient);

        // store fingerprint
        DuplicateFingerprint df = new DuplicateFingerprint();
        df.setOrganizationId(actor.getOrganizationId());
        df.setFingerprintType("PATIENT_IDENTITY");
        df.setFingerprintValue(fingerprint);
        df.setObjectType("Patient");
        df.setObjectId(patient.getId());
        dupRepo.save(df);

        auditService.log(actor.getId(), actor.getUsername(), dupExists ? "PATIENT_REGISTER_DUPLICATE_WARN" : "PATIENT_REGISTER",
                "Patient", String.valueOf(patient.getId()), actor.getOrganizationId(), ip, workstationId, null,
                "{\"mrn\":\"" + patient.getMedicalRecordNumber() + "\"}");

        return patient;
    }

    @Transactional(readOnly = true)
    public Patient getById(User actor, Long patientId) {
        roleGuard.require(actor, Role.FRONT_DESK, Role.CLINICIAN, Role.BILLING, Role.QUALITY, Role.ADMIN);
        return patientRepo.findByOrganizationIdAndId(actor.getOrganizationId(), patientId)
                .orElseThrow(() -> new NotFoundException("Patient not found: " + patientId));
    }

    @Transactional(readOnly = true)
    public Page<Patient> list(User actor, String q, String archived, Pageable pageable) {
        roleGuard.require(actor, Role.FRONT_DESK, Role.CLINICIAN, Role.BILLING, Role.QUALITY, Role.ADMIN);
        String search = (q != null && !q.isBlank()) ? q.strip() : null;
        boolean archivedOnly = "true".equalsIgnoreCase(archived);
        boolean showArchived = archivedOnly || "all".equalsIgnoreCase(archived);
        return patientRepo.findFiltered(actor.getOrganizationId(), search, showArchived, archivedOnly, pageable);
    }

    @Transactional
    public Patient archive(User actor, Long patientId, String ip, String workstationId) {
        roleGuard.require(actor, Role.ADMIN);
        Patient p = getById(actor, patientId);
        p.setArchivedAt(Instant.now());
        p = patientRepo.save(p);
        auditService.log(actor.getId(), actor.getUsername(), "PATIENT_ARCHIVE",
                "Patient", String.valueOf(patientId), actor.getOrganizationId(), ip, workstationId, null, null);
        return p;
    }

    public record RevealResult(String firstName, String lastName, String phone, String address, String emergencyContactPhone) {}

    @Transactional
    public RevealResult reveal(User actor, Long patientId, String ip, String workstationId) {
        // Object-level authorization: only roles that legitimately need sensitive PII may reveal.
        // Protected-case patients additionally require ADMIN or QUALITY.
        Role r = actor.getRole();
        if (!roleGuard.hasRole(actor, Role.ADMIN, Role.CLINICIAN, Role.BILLING, Role.QUALITY, Role.FRONT_DESK)) {
            auditService.log(actor.getId(), actor.getUsername(), "PATIENT_PII_REVEAL_DENIED",
                    "Patient", String.valueOf(patientId), actor.getOrganizationId(), ip, workstationId,
                    null, "{\"reason\":\"role_not_permitted\",\"role\":\"" + r + "\"}");
            throw new com.rescuehub.exception.ForbiddenException("Role not permitted to reveal patient PII");
        }
        Patient p = patientRepo.findByOrganizationIdAndId(actor.getOrganizationId(), patientId)
                .orElseThrow(() -> new NotFoundException("Patient not found: " + patientId));
        if (p.isProtectedCase() && r != Role.ADMIN && r != Role.QUALITY) {
            auditService.log(actor.getId(), actor.getUsername(), "PATIENT_PII_REVEAL_DENIED",
                    "Patient", String.valueOf(patientId), actor.getOrganizationId(), ip, workstationId,
                    null, "{\"reason\":\"protected_case\",\"role\":\"" + r + "\"}");
            throw new com.rescuehub.exception.ForbiddenException("Protected-case patient reveal restricted to ADMIN/QUALITY");
        }

        String fn = p.getFirstNameCiphertext() != null ? cryptoService.decrypt(p.getFirstNameCiphertext(), p.getFirstNameIv()) : null;
        String ln = p.getLastNameCiphertext() != null ? cryptoService.decrypt(p.getLastNameCiphertext(), p.getLastNameIv()) : null;
        String phone = p.getPhoneCiphertext() != null ? cryptoService.decrypt(p.getPhoneCiphertext(), p.getPhoneIv()) : null;
        String addr = p.getAddressCiphertext() != null ? cryptoService.decrypt(p.getAddressCiphertext(), p.getAddressIv()) : null;
        String ecp = p.getEmergencyContactPhoneCiphertext() != null
                ? cryptoService.decrypt(p.getEmergencyContactPhoneCiphertext(), p.getEmergencyContactPhoneIv()) : null;

        // Audit with explicit list of revealed fields (no sensitive values in the audit payload itself).
        StringBuilder fields = new StringBuilder("[");
        if (fn != null) fields.append("\"firstName\",");
        if (ln != null) fields.append("\"lastName\",");
        if (phone != null) fields.append("\"phone\",");
        if (addr != null) fields.append("\"address\",");
        if (ecp != null) fields.append("\"emergencyContactPhone\",");
        if (fields.length() > 1) fields.setLength(fields.length() - 1);
        fields.append("]");
        String after = "{\"fieldsRevealed\":" + fields + ",\"isProtectedCase\":" + p.isProtectedCase() + "}";
        auditService.log(actor.getId(), actor.getUsername(), "PATIENT_PII_REVEAL",
                "Patient", String.valueOf(patientId), actor.getOrganizationId(), ip, workstationId, null, after);
        return new RevealResult(fn, ln, phone, addr, ecp);
    }
}
