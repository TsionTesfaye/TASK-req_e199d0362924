package com.rescuehub.repository;

import com.rescuehub.entity.PatientIdentityVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PatientIdentityVerificationRepository extends JpaRepository<PatientIdentityVerification, Long> {
    List<PatientIdentityVerification> findByPatientId(Long patientId);
}
