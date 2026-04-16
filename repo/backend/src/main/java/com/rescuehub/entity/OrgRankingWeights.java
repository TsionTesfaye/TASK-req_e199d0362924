package com.rescuehub.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "org_ranking_weights")
public class OrgRankingWeights {

    @Id
    private Long organizationId;

    @Column(nullable = false) private double recency      = 1.0;
    @Column(nullable = false) private double favorites    = 2.0;
    @Column(nullable = false) private double comments     = 1.5;
    @Column(nullable = false) private double moderatorBoost = 5.0;
    @Column(nullable = false) private double coldStartBase  = 0.5;
    @Column(nullable = false) private Instant updatedAt;

    @PrePersist @PreUpdate
    protected void onUpdate() { updatedAt = Instant.now(); }

    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public double getRecency() { return recency; }
    public void setRecency(double recency) { this.recency = recency; }
    public double getFavorites() { return favorites; }
    public void setFavorites(double favorites) { this.favorites = favorites; }
    public double getComments() { return comments; }
    public void setComments(double comments) { this.comments = comments; }
    public double getModeratorBoost() { return moderatorBoost; }
    public void setModeratorBoost(double moderatorBoost) { this.moderatorBoost = moderatorBoost; }
    public double getColdStartBase() { return coldStartBase; }
    public void setColdStartBase(double coldStartBase) { this.coldStartBase = coldStartBase; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
