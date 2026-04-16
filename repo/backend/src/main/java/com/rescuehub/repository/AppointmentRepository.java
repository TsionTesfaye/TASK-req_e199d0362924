package com.rescuehub.repository;

import com.rescuehub.entity.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    Optional<Appointment> findByOrganizationIdAndId(Long orgId, Long id);
    Page<Appointment> findByOrganizationId(Long orgId, Pageable pageable);
    Page<Appointment> findByOrganizationIdAndScheduledDate(Long orgId, LocalDate date, Pageable pageable);
    Page<Appointment> findByOrganizationIdAndPatientId(Long orgId, Long patientId, Pageable pageable);
}
