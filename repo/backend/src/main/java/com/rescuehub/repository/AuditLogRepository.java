package com.rescuehub.repository;

import com.rescuehub.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByOrganizationId(Long orgId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query(
        "SELECT e FROM AuditLog e WHERE e.organizationId = :orgId " +
        "AND (:q IS NULL OR LOWER(e.actorUsernameSnapshot) LIKE LOWER(CONCAT('%', :q, '%')) " +
        "     OR LOWER(e.actionCode) LIKE LOWER(CONCAT('%', :q, '%')))")
    Page<AuditLog> findFiltered(Long orgId, String q, Pageable pageable);

    Page<AuditLog> findByOrganizationIdAndActionCodeOrderByCreatedAtDesc(Long orgId, String actionCode, Pageable pageable);
}
