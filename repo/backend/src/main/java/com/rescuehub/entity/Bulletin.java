package com.rescuehub.entity;

import com.rescuehub.enums.BulletinStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "bulletin")
public class Bulletin {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long organizationId;
    @Column(nullable = false) private String title;
    @Column(nullable = false, columnDefinition = "LONGTEXT") private String body;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private BulletinStatus status = BulletinStatus.DRAFT;
    @Column(nullable = false) private Long createdByUserId;
    private Long moderatedByUserId;
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
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public BulletinStatus getStatus() { return status; }
    public void setStatus(BulletinStatus status) { this.status = status; }
    public Long getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(Long createdByUserId) { this.createdByUserId = createdByUserId; }
    public Long getModeratedByUserId() { return moderatedByUserId; }
    public void setModeratedByUserId(Long moderatedByUserId) { this.moderatedByUserId = moderatedByUserId; }
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
