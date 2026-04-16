package com.rescuehub.entity;

import com.rescuehub.enums.BackupStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "backup_run")
public class BackupRun {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long organizationId;
    @Column(nullable = false) private String backupType;
    private String outputPath;
    private Instant retentionExpiresAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private BackupStatus status = BackupStatus.RUNNING;
    @Column(nullable = false) private Instant createdAt;
    private Instant completedAt;

    @PrePersist protected void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public String getBackupType() { return backupType; }
    public void setBackupType(String backupType) { this.backupType = backupType; }
    public String getOutputPath() { return outputPath; }
    public void setOutputPath(String outputPath) { this.outputPath = outputPath; }
    public Instant getRetentionExpiresAt() { return retentionExpiresAt; }
    public void setRetentionExpiresAt(Instant retentionExpiresAt) { this.retentionExpiresAt = retentionExpiresAt; }
    public BackupStatus getStatus() { return status; }
    public void setStatus(BackupStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
