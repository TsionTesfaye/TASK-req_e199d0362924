package com.rescuehub.repository;

import com.rescuehub.entity.BillingRule;
import com.rescuehub.enums.BillingRuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BillingRuleRepository extends JpaRepository<BillingRule, Long> {
    List<BillingRule> findByOrganizationIdAndIsActiveOrderByPriorityAsc(Long orgId, boolean isActive);
    List<BillingRule> findByOrganizationIdAndRuleTypeAndIsActiveOrderByPriorityAsc(Long orgId, BillingRuleType ruleType, boolean isActive);
}
