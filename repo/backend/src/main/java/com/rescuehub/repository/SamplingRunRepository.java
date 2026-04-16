package com.rescuehub.repository;

import com.rescuehub.entity.SamplingRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SamplingRunRepository extends JpaRepository<SamplingRun, Long> {
    Page<SamplingRun> findByOrganizationId(Long orgId, Pageable pageable);
    boolean existsByOrganizationIdAndPeriod(Long orgId, String period);
}
