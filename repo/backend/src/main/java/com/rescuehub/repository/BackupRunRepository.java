package com.rescuehub.repository;

import com.rescuehub.entity.BackupRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface BackupRunRepository extends JpaRepository<BackupRun, Long> {
    Page<BackupRun> findByOrganizationId(Long orgId, Pageable pageable);
    List<BackupRun> findByOrganizationIdAndRetentionExpiresAtBefore(Long orgId, Instant now);
    List<BackupRun> findByOrganizationIdAndCreatedAtAfter(Long orgId, Instant after);
}
