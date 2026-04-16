package com.rescuehub.repository;

import com.rescuehub.entity.Bulletin;
import com.rescuehub.enums.BulletinStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface BulletinRepository extends JpaRepository<Bulletin, Long> {
    Optional<Bulletin> findByOrganizationIdAndId(Long orgId, Long id);
    Page<Bulletin> findByOrganizationId(Long orgId, Pageable pageable);

    @Query("SELECT b FROM Bulletin b WHERE b.organizationId = :orgId AND b.status NOT IN :excluded AND (LOWER(b.title) LIKE %:q% OR LOWER(b.body) LIKE %:q%)")
    Page<Bulletin> searchVisible(@Param("orgId") Long orgId, @Param("q") String q,
                                  @Param("excluded") List<BulletinStatus> excluded, Pageable pageable);

    Page<Bulletin> findByOrganizationIdAndStatus(Long orgId, BulletinStatus status, Pageable pageable);

    @Modifying
    @Query("UPDATE Bulletin b SET b.favoriteCount = GREATEST(0, b.favoriteCount + :delta) WHERE b.id = :id AND b.organizationId = :orgId")
    void adjustFavoriteCount(@Param("orgId") Long orgId, @Param("id") Long id, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE Bulletin b SET b.commentCount = b.commentCount + 1 WHERE b.id = :id AND b.organizationId = :orgId")
    void incrementCommentCount(@Param("orgId") Long orgId, @Param("id") Long id);

    @Modifying
    @Query("UPDATE Bulletin b SET b.viewCount = b.viewCount + 1 WHERE b.id = :id AND b.organizationId = :orgId")
    void incrementViewCount(@Param("orgId") Long orgId, @Param("id") Long id);
}
