package com.rescuehub.repository;

import com.rescuehub.entity.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Optional<Favorite> findByOrganizationIdAndUserIdAndContentTypeAndContentId(Long orgId, Long userId, String contentType, Long contentId);
    boolean existsByOrganizationIdAndUserIdAndContentTypeAndContentId(Long orgId, Long userId, String contentType, Long contentId);
    long countByContentTypeAndContentId(String contentType, Long contentId);
    Page<Favorite> findByOrganizationIdAndUserId(Long orgId, Long userId, Pageable pageable);
}
