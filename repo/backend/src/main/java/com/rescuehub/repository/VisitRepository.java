package com.rescuehub.repository;

import com.rescuehub.entity.Visit;
import com.rescuehub.enums.VisitStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VisitRepository extends JpaRepository<Visit, Long> {
    Optional<Visit> findByOrganizationIdAndId(Long orgId, Long id);
    Page<Visit> findByOrganizationId(Long orgId, Pageable pageable);
    Page<Visit> findByOrganizationIdAndPatientId(Long orgId, Long patientId, Pageable pageable);
    boolean existsByOrganizationIdAndIdAndStatus(Long orgId, Long id, VisitStatus status);
    Optional<Visit> findByIdAndStatus(Long id, VisitStatus status);
    List<Visit> findByOrganizationIdAndStatus(Long orgId, VisitStatus status);

    // for 7-day frequency anomaly: count closed visits for patient in last 7 days
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(v) FROM Visit v WHERE v.organizationId = :orgId AND v.patientId = :patientId AND v.closedAt >= :since AND v.status = :status")
    long countRecentClosedVisits(Long orgId, Long patientId, java.time.Instant since, @org.springframework.data.repository.query.Param("status") com.rescuehub.enums.VisitStatus status);
}
