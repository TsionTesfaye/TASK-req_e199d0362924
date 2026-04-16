package com.rescuehub.repository;

import com.rescuehub.entity.RefundRequest;
import com.rescuehub.enums.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;

public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {
    List<RefundRequest> findByInvoiceId(Long invoiceId);

    @Query("SELECT COALESCE(SUM(r.refundAmount),0) FROM RefundRequest r WHERE r.invoiceId = :invoiceId AND r.status = :status")
    BigDecimal sumExecutedRefundsByInvoiceId(@Param("invoiceId") Long invoiceId, @Param("status") RefundStatus status);
}
