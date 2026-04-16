package com.rescuehub.repository;

import com.rescuehub.entity.CorrectiveAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CorrectiveActionRepository extends JpaRepository<CorrectiveAction, Long> {
    Optional<CorrectiveAction> findByOrganizationIdAndId(Long orgId, Long id);
    Page<CorrectiveAction> findByOrganizationId(Long orgId, Pageable pageable);
}
