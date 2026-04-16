package com.rescuehub.controller;

import com.rescuehub.entity.Patient;
import com.rescuehub.entity.User;
import com.rescuehub.security.SecurityUtils;
import com.rescuehub.service.PatientService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    /**
     * Safe display DTO for patient list and detail responses.
     * Never includes ciphertext fields or raw byte arrays.
     * Encrypted PII (firstName, lastName, phone, address) is only available via GET /{id}/reveal.
     */
    record PatientResponse(
            Long id, String medicalRecordNumber, LocalDate dateOfBirth, String sex,
            String phoneLast4, String emergencyContactName,
            boolean isMinor, boolean isProtectedCase,
            Instant createdAt, Instant archivedAt) {

        static PatientResponse from(Patient p) {
            return new PatientResponse(
                    p.getId(), p.getMedicalRecordNumber(), p.getDateOfBirth(), p.getSex(),
                    p.getPhoneLast4(), p.getEmergencyContactName(),
                    p.isMinor(), p.isProtectedCase(),
                    p.getCreatedAt(), p.getArchivedAt());
        }
    }

    record RegisterRequest(
            @NotBlank String firstName, @NotBlank String lastName,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateOfBirth,
            String sex, String phone, String address,
            String emergencyContactName, String emergencyContactPhone,
            boolean isMinor, boolean isProtectedCase) {}

    @PostMapping
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest req, HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        Patient patient = patientService.register(actor, req.firstName(), req.lastName(), req.dateOfBirth(),
                req.sex(), req.phone(), req.address(), req.emergencyContactName(), req.emergencyContactPhone(),
                req.isMinor(), req.isProtectedCase(), request.getRemoteAddr(), SecurityUtils.currentWorkstationId());
        return ApiResponse.data(PatientResponse.from(patient));
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     @RequestParam(required = false) String q,
                                     @RequestParam(required = false) String archived) {
        User actor = SecurityUtils.currentUser();
        Page<Patient> patients = patientService.list(actor, q, archived, PageBounds.of(page, size));
        List<PatientResponse> dtos = patients.getContent().stream().map(PatientResponse::from).toList();
        return ApiResponse.list(dtos, patients.getTotalElements(), page, size);
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Long id) {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(PatientResponse.from(patientService.getById(actor, id)));
    }

    @DeleteMapping("/{id}/archive")
    public Map<String, Object> archive(@PathVariable Long id, HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(PatientResponse.from(
                patientService.archive(actor, id, request.getRemoteAddr(), SecurityUtils.currentWorkstationId())));
    }

    @GetMapping("/{id}/reveal")
    public Map<String, Object> reveal(@PathVariable Long id, HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        PatientService.RevealResult result = patientService.reveal(actor, id, request.getRemoteAddr(), SecurityUtils.currentWorkstationId());
        return ApiResponse.data(result);
    }

    record VerifyIdentityRequest(@NotBlank String documentType, @NotBlank String documentLast4, String note) {}

    @PostMapping("/{id}/verify-identity")
    public Map<String, Object> verifyIdentity(@PathVariable Long id,
                                               @Valid @RequestBody VerifyIdentityRequest req,
                                               HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(patientService.verifyIdentity(actor, id,
                req.documentType(), req.documentLast4(), req.note(),
                request.getRemoteAddr(), SecurityUtils.currentWorkstationId()));
    }

    @GetMapping("/{id}/verifications")
    public Map<String, Object> listVerifications(@PathVariable Long id) {
        User actor = SecurityUtils.currentUser();
        var list = patientService.listVerifications(actor, id);
        return ApiResponse.list(list, list.size(), 0, list.size());
    }
}
