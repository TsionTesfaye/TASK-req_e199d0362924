package com.rescuehub.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "quality_override")
public class QualityOverride {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long qualityRuleResultId;
    @Column(nullable = false) private Long overriddenByUserId;
    @Column(nullable = false) private String overrideReasonCode;
    @Column(nullable = false, columnDefinition = "TEXT") private String overrideNote;
    @Column(nullable = false) private Instant createdAt;

    @PrePersist protected void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getQualityRuleResultId() { return qualityRuleResultId; }
    public void setQualityRuleResultId(Long qualityRuleResultId) { this.qualityRuleResultId = qualityRuleResultId; }
    public Long getOverriddenByUserId() { return overriddenByUserId; }
    public void setOverriddenByUserId(Long overriddenByUserId) { this.overriddenByUserId = overriddenByUserId; }
    public String getOverrideReasonCode() { return overrideReasonCode; }
    public void setOverrideReasonCode(String overrideReasonCode) { this.overrideReasonCode = overrideReasonCode; }
    public String getOverrideNote() { return overrideNote; }
    public void setOverrideNote(String overrideNote) { this.overrideNote = overrideNote; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
