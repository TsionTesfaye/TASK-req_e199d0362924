package com.rescuehub.repository;

import com.rescuehub.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByOrganizationIdAndId(Long orgId, Long id);
    Optional<Invoice> findByVisitId(Long visitId);
    Page<Invoice> findByOrganizationId(Long orgId, Pageable pageable);
    boolean existsByVisitId(Long visitId);
}
