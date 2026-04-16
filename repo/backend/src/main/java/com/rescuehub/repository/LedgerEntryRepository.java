package com.rescuehub.repository;

import com.rescuehub.entity.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByOrganizationId(Long orgId);
    Page<LedgerEntry> findByOrganizationId(Long orgId, Pageable pageable);
    List<LedgerEntry> findByInvoiceId(Long invoiceId);
}
