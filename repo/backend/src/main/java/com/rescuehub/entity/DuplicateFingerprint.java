package com.rescuehub.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "duplicate_fingerprint")
public class DuplicateFingerprint {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long organizationId;
    @Column(nullable = false) private String fingerprintType;
    @Column(nullable = false) private String fingerprintValue;
    @Column(nullable = false) private String objectType;
    @Column(nullable = false) private Long objectId;
    @Column(nullable = false) private Instant createdAt;

    @PrePersist protected void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public String getFingerprintType() { return fingerprintType; }
    public void setFingerprintType(String fingerprintType) { this.fingerprintType = fingerprintType; }
    public String getFingerprintValue() { return fingerprintValue; }
    public void setFingerprintValue(String fingerprintValue) { this.fingerprintValue = fingerprintValue; }
    public String getObjectType() { return objectType; }
    public void setObjectType(String objectType) { this.objectType = objectType; }
    public Long getObjectId() { return objectId; }
    public void setObjectId(Long objectId) { this.objectId = objectId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
