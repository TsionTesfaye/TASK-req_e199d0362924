package com.rescuehub.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ranked_content_entry")
public class RankedContentEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long organizationId;
    @Column(nullable = false) private String contentType;
    @Column(nullable = false) private Long contentId;
    @Column(columnDefinition = "JSON") private String weightingSnapshotJson;
    @Column(nullable = false) private BigDecimal score = BigDecimal.ZERO;
    private Long promotedByUserId;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    @PrePersist protected void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getContentId() { return contentId; }
    public void setContentId(Long contentId) { this.contentId = contentId; }
    public String getWeightingSnapshotJson() { return weightingSnapshotJson; }
    public void setWeightingSnapshotJson(String weightingSnapshotJson) { this.weightingSnapshotJson = weightingSnapshotJson; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public Long getPromotedByUserId() { return promotedByUserId; }
    public void setPromotedByUserId(Long promotedByUserId) { this.promotedByUserId = promotedByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
