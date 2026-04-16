package com.rescuehub.repository;

import com.rescuehub.entity.RankedContentEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RankedContentEntryRepository extends JpaRepository<RankedContentEntry, Long> {
    Optional<RankedContentEntry> findByOrganizationIdAndContentTypeAndContentId(Long orgId, String contentType, Long contentId);
    Page<RankedContentEntry> findByOrganizationIdOrderByScoreDesc(Long orgId, Pageable pageable);
}
