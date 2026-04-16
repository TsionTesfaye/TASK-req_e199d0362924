package com.rescuehub.repository;

import com.rescuehub.entity.QualityOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface QualityOverrideRepository extends JpaRepository<QualityOverride, Long> {
    Optional<QualityOverride> findByQualityRuleResultId(Long resultId);
}
