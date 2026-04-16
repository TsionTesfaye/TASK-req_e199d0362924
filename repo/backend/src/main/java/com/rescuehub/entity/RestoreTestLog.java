package com.rescuehub.entity;

import com.rescuehub.enums.RestoreTestResult;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "restore_test_log")
public class RestoreTestLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long organizationId;
    @Column(nullable = false) private Long performedByUserId;
    @Column(nullable = false) private Long backupRunId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private RestoreTestResult result;
    @Column(columnDefinition = "TEXT") private String note;
    @Column(nullable = false) private Instant performedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public Long getPerformedByUserId() { return performedByUserId; }
    public void setPerformedByUserId(Long performedByUserId) { this.performedByUserId = performedByUserId; }
    public Long getBackupRunId() { return backupRunId; }
    public void setBackupRunId(Long backupRunId) { this.backupRunId = backupRunId; }
    public RestoreTestResult getResult() { return result; }
    public void setResult(RestoreTestResult result) { this.result = result; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Instant getPerformedAt() { return performedAt; }
    public void setPerformedAt(Instant performedAt) { this.performedAt = performedAt; }
}
