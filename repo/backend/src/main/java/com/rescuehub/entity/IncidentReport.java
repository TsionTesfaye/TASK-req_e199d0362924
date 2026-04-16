package com.rescuehub.entity;

import com.rescuehub.enums.IncidentStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "incident_report")
public class IncidentReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long organizationId;
    private Long submittedByUserId;
    private String externalReporterName;
    @Column(nullable = false) private boolean isAnonymous = false;
    private String subjectAgeGroup;
    @Column(nullable = false) private boolean involvesMinor = false;
    @Column(nullable = false) private boolean isProtectedCase = false;
    @Column(nullable = false) private String category;
    @Column(nullable = false, columnDefinition = "LONGTEXT") private String description;
    @Column(nullable = false, columnDefinition = "TEXT") private String approximateLocationText;
    private String neighborhood;
    private String nearestCrossStreets;
    private byte[] exactLocationCiphertext;
    private byte[] exactLocationIv;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private IncidentStatus status = IncidentStatus.SUBMITTED;
    @Column(nullable = false) private long favoriteCount = 0;
    @Column(nullable = false) private long commentCount = 0;
    @Column(nullable = false) private long viewCount = 0;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    @Version private Long version;

    @PrePersist protected void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public Long getSubmittedByUserId() { return submittedByUserId; }
    public void setSubmittedByUserId(Long submittedByUserId) { this.submittedByUserId = submittedByUserId; }
    public String getExternalReporterName() { return externalReporterName; }
    public void setExternalReporterName(String externalReporterName) { this.externalReporterName = externalReporterName; }
    public boolean isAnonymous() { return isAnonymous; }
    public void setAnonymous(boolean anonymous) { isAnonymous = anonymous; }
    public String getSubjectAgeGroup() { return subjectAgeGroup; }
    public void setSubjectAgeGroup(String subjectAgeGroup) { this.subjectAgeGroup = subjectAgeGroup; }
    public boolean isInvolvesMinor() { return involvesMinor; }
    public void setInvolvesMinor(boolean involvesMinor) { this.involvesMinor = involvesMinor; }
    public boolean isProtectedCase() { return isProtectedCase; }
    public void setProtectedCase(boolean protectedCase) { isProtectedCase = protectedCase; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getApproximateLocationText() { return approximateLocationText; }
    public void setApproximateLocationText(String approximateLocationText) { this.approximateLocationText = approximateLocationText; }
    public String getNeighborhood() { return neighborhood; }
    public void setNeighborhood(String neighborhood) { this.neighborhood = neighborhood; }
    public String getNearestCrossStreets() { return nearestCrossStreets; }
    public void setNearestCrossStreets(String nearestCrossStreets) { this.nearestCrossStreets = nearestCrossStreets; }
    public byte[] getExactLocationCiphertext() { return exactLocationCiphertext; }
    public void setExactLocationCiphertext(byte[] exactLocationCiphertext) { this.exactLocationCiphertext = exactLocationCiphertext; }
    public byte[] getExactLocationIv() { return exactLocationIv; }
    public void setExactLocationIv(byte[] exactLocationIv) { this.exactLocationIv = exactLocationIv; }
    public IncidentStatus getStatus() { return status; }
    public void setStatus(IncidentStatus status) { this.status = status; }
    public long getFavoriteCount() { return favoriteCount; }
    public void setFavoriteCount(long favoriteCount) { this.favoriteCount = favoriteCount; }
    public long getCommentCount() { return commentCount; }
    public void setCommentCount(long commentCount) { this.commentCount = commentCount; }
    public long getViewCount() { return viewCount; }
    public void setViewCount(long viewCount) { this.viewCount = viewCount; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
