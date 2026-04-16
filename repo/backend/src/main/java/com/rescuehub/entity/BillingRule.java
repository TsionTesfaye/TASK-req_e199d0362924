package com.rescuehub.entity;

import com.rescuehub.enums.BillingRuleType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "billing_rule")
public class BillingRule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long organizationId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private BillingRuleType ruleType;
    @Column(nullable = false) private String code;
    @Column(nullable = false) private String name;
    private BigDecimal amount;
    private BigDecimal percentage;
    private BigDecimal taxRate;
    @Column(columnDefinition = "JSON") private String packageDefinitionJson;
    @Column(nullable = false) private boolean isActive = true;
    @Column(nullable = false) private int priority = 100;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    @PrePersist protected void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public BillingRuleType getRuleType() { return ruleType; }
    public void setRuleType(BillingRuleType ruleType) { this.ruleType = ruleType; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getPercentage() { return percentage; }
    public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }
    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
    public String getPackageDefinitionJson() { return packageDefinitionJson; }
    public void setPackageDefinitionJson(String packageDefinitionJson) { this.packageDefinitionJson = packageDefinitionJson; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
