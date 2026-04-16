package com.rescuehub.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sampling_run")
public class SamplingRun {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long organizationId;
    @Column(nullable = false) private String period;
    @Column(nullable = false) private String seed;
    @Column(nullable = false) private int percentage = 5;
    @Column(nullable = false) private Instant createdAt;

    @PrePersist protected void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public String getSeed() { return seed; }
    public void setSeed(String seed) { this.seed = seed; }
    public int getPercentage() { return percentage; }
    public void setPercentage(int percentage) { this.percentage = percentage; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
