package com.rescuehub.repository;

import com.rescuehub.entity.DailyClose;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface DailyCloseRepository extends JpaRepository<DailyClose, Long> {
    Optional<DailyClose> findByOrganizationIdAndBusinessDate(Long orgId, LocalDate businessDate);
    boolean existsByOrganizationIdAndBusinessDate(Long orgId, LocalDate businessDate);
    Page<DailyClose> findByOrganizationId(Long orgId, Pageable pageable);
}
