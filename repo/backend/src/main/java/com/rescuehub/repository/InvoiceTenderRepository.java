package com.rescuehub.repository;

import com.rescuehub.entity.InvoiceTender;
import com.rescuehub.enums.TenderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;

public interface InvoiceTenderRepository extends JpaRepository<InvoiceTender, Long> {
    List<InvoiceTender> findByInvoiceId(Long invoiceId);

    @Query("SELECT COALESCE(SUM(t.amount),0) FROM InvoiceTender t WHERE t.invoiceId = :invoiceId AND t.status NOT IN (:voided, :refunded)")
    BigDecimal sumActiveAmountByInvoiceId(@Param("invoiceId") Long invoiceId,
                                          @Param("voided") TenderStatus voided,
                                          @Param("refunded") TenderStatus refunded);
}
