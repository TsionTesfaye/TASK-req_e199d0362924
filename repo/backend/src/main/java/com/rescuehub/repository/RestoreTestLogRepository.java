package com.rescuehub.repository;

import com.rescuehub.entity.RestoreTestLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestoreTestLogRepository extends JpaRepository<RestoreTestLog, Long> {
    Page<RestoreTestLog> findByOrganizationId(Long orgId, Pageable pageable);
}
