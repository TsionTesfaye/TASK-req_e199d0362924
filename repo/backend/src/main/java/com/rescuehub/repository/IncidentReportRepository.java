package com.rescuehub.repository;

import com.rescuehub.entity.IncidentReport;
import com.rescuehub.enums.IncidentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface IncidentReportRepository extends JpaRepository<IncidentReport, Long> {
    Optional<IncidentReport> findByOrganizationIdAndId(Long orgId, Long id);
    Page<IncidentReport> findByOrganizationId(Long orgId, Pageable pageable);

    @Query("SELECT i FROM IncidentReport i WHERE i.organizationId = :orgId AND i.status NOT IN :excluded AND (LOWER(i.description) LIKE %:q% OR LOWER(i.category) LIKE %:q% OR LOWER(i.approximateLocationText) LIKE %:q%)")
    Page<IncidentReport> searchVisible(@Param("orgId") Long orgId, @Param("q") String q,
                                       @Param("excluded") List<IncidentStatus> excluded, Pageable pageable);

    Page<IncidentReport> findByOrganizationIdAndStatus(Long orgId, IncidentStatus status, Pageable pageable);

    @Modifying
    @Query("UPDATE IncidentReport i SET i.favoriteCount = GREATEST(0, i.favoriteCount + :delta) WHERE i.id = :id AND i.organizationId = :orgId")
    void adjustFavoriteCount(@Param("orgId") Long orgId, @Param("id") Long id, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE IncidentReport i SET i.commentCount = i.commentCount + 1 WHERE i.id = :id AND i.organizationId = :orgId")
    void incrementCommentCount(@Param("orgId") Long orgId, @Param("id") Long id);

    @Modifying
    @Query("UPDATE IncidentReport i SET i.viewCount = i.viewCount + 1 WHERE i.id = :id AND i.organizationId = :orgId")
    void incrementViewCount(@Param("orgId") Long orgId, @Param("id") Long id);
}
