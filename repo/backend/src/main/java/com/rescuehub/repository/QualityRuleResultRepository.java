package com.rescuehub.repository;

import com.rescuehub.entity.QualityRuleResult;
import com.rescuehub.enums.QualityResultStatus;
import com.rescuehub.enums.QualitySeverity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface QualityRuleResultRepository extends JpaRepository<QualityRuleResult, Long> {
    List<QualityRuleResult> findByVisitIdAndSeverityAndStatus(Long visitId, QualitySeverity severity, QualityResultStatus status);
    Page<QualityRuleResult> findByOrganizationId(Long orgId, Pageable pageable);
    Optional<QualityRuleResult> findByOrganizationIdAndId(Long orgId, Long id);
    List<QualityRuleResult> findByVisitId(Long visitId);
}
