package com.rescuehub.repository;

import com.rescuehub.entity.RetentionPolicyHold;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RetentionPolicyHoldRepository extends JpaRepository<RetentionPolicyHold, Long> {
    List<RetentionPolicyHold> findByPatientId(Long patientId);
}
