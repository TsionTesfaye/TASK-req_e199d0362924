package com.rescuehub.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "visit_charge")
public class VisitCharge {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long visitId;
    @Column(nullable = false) private String serviceCode;
    @Column(nullable = false) private String description;
    @Column(nullable = false) private String pricingSourceType;
    @Column(nullable = false) private BigDecimal unitPrice;
    @Column(nullable = false) private int quantity = 1;
    @Column(nullable = false) private BigDecimal lineTotal;
    @Column(nullable = false) private boolean taxable = true;
    @Column(nullable = false) private Instant createdAt;

    @PrePersist protected void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getVisitId() { return visitId; }
    public void setVisitId(Long visitId) { this.visitId = visitId; }
    public String getServiceCode() { return serviceCode; }
    public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPricingSourceType() { return pricingSourceType; }
    public void setPricingSourceType(String pricingSourceType) { this.pricingSourceType = pricingSourceType; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
    public boolean isTaxable() { return taxable; }
    public void setTaxable(boolean taxable) { this.taxable = taxable; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
