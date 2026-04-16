package com.rescuehub.service;

import com.rescuehub.entity.Appointment;
import com.rescuehub.entity.User;
import com.rescuehub.enums.AppointmentStatus;
import com.rescuehub.enums.Role;
import com.rescuehub.exception.NotFoundException;
import com.rescuehub.repository.AppointmentRepository;
import com.rescuehub.repository.PatientRepository;
import com.rescuehub.repository.UserRepository;
import com.rescuehub.security.RoleGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

@Service
public class AppointmentService {

    private final AppointmentRepository repo;
    private final PatientRepository patientRepo;
    private final UserRepository userRepo;
    private final AuditService auditService;
    private final RoleGuard roleGuard;

    public AppointmentService(AppointmentRepository repo, PatientRepository patientRepo,
                               UserRepository userRepo, AuditService auditService, RoleGuard roleGuard) {
        this.repo = repo;
        this.patientRepo = patientRepo;
        this.userRepo = userRepo;
        this.auditService = auditService;
        this.roleGuard = roleGuard;
    }

    @Transactional
    public Appointment create(User actor, Long patientId, LocalDate date, java.time.LocalTime time,
                               Long clinicianId, String ip, String workstationId) {
        roleGuard.require(actor, Role.FRONT_DESK, Role.CLINICIAN, Role.ADMIN);
        // Validate patient exists in actor's organization (object integrity check)
        patientRepo.findByOrganizationIdAndId(actor.getOrganizationId(), patientId)
                .orElseThrow(() -> new NotFoundException("Patient not found: " + patientId));
        // Validate clinician belongs to same organization
        if (clinicianId != null) {
            User clinician = userRepo.findById(clinicianId)
                    .orElseThrow(() -> new NotFoundException("Clinician not found: " + clinicianId));
            if (!clinician.getOrganizationId().equals(actor.getOrganizationId())) {
                throw new NotFoundException("Clinician not found: " + clinicianId);
            }
        }
        Appointment appt = new Appointment();
        appt.setOrganizationId(actor.getOrganizationId());
        appt.setPatientId(patientId);
        appt.setScheduledDate(date);
        appt.setScheduledTime(time);
        appt.setClinicianUserId(clinicianId);
        appt.setStatus(AppointmentStatus.SCHEDULED);
        appt = repo.save(appt);
        auditService.log(actor.getId(), actor.getUsername(), "APPOINTMENT_CREATE",
                "Appointment", String.valueOf(appt.getId()), actor.getOrganizationId(), ip, workstationId, null, null);
        return appt;
    }

    @Transactional(readOnly = true)
    public Page<Appointment> list(User actor, LocalDate date, Pageable pageable) {
        roleGuard.require(actor, Role.FRONT_DESK, Role.CLINICIAN, Role.ADMIN);
        if (date != null) {
            return repo.findByOrganizationIdAndScheduledDate(actor.getOrganizationId(), date, pageable);
        }
        return repo.findByOrganizationId(actor.getOrganizationId(), pageable);
    }

    @Transactional(readOnly = true)
    public Appointment getById(User actor, Long id) {
        roleGuard.require(actor, Role.FRONT_DESK, Role.CLINICIAN, Role.ADMIN);
        return repo.findByOrganizationIdAndId(actor.getOrganizationId(), id)
                .orElseThrow(() -> new NotFoundException("Appointment not found"));
    }

    @Transactional
    public Appointment updateStatus(User actor, Long id, AppointmentStatus status, String ip, String workstationId) {
        roleGuard.require(actor, Role.FRONT_DESK, Role.CLINICIAN, Role.ADMIN);
        Appointment appt = getById(actor, id);
        String before = "{\"status\":\"" + appt.getStatus() + "\"}";
        appt.setStatus(status);
        if (status == AppointmentStatus.ARCHIVED) appt.setArchivedAt(Instant.now());
        appt = repo.save(appt);
        auditService.log(actor.getId(), actor.getUsername(), "APPOINTMENT_UPDATE",
                "Appointment", String.valueOf(id), actor.getOrganizationId(), ip, workstationId, before, null);
        return appt;
    }
}
