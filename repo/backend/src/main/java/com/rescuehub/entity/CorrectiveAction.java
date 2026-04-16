package com.rescuehub.entity;

import com.rescuehub.enums.CorrectiveActionStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "corrective_action")
public class CorrectiveAction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long organizationId;
    private Long relatedVisitId;
    private Long relatedRuleResultId;
    private Long assignedToUserId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private CorrectiveActionStatus status = CorrectiveActionStatus.OPEN;
    @Column(nullable = false, columnDefinition = "TEXT") private String description;
    @Column(columnDefinition = "TEXT") private String resolutionNote;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    private Instant closedAt;

    @PrePersist protected void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public Long getRelatedVisitId() { return relatedVisitId; }
    public void setRelatedVisitId(Long relatedVisitId) { this.relatedVisitId = relatedVisitId; }
    public Long getRelatedRuleResultId() { return relatedRuleResultId; }
    public void setRelatedRuleResultId(Long relatedRuleResultId) { this.relatedRuleResultId = relatedRuleResultId; }
    public Long getAssignedToUserId() { return assignedToUserId; }
    public void setAssignedToUserId(Long assignedToUserId) { this.assignedToUserId = assignedToUserId; }
    public CorrectiveActionStatus getStatus() { return status; }
    public void setStatus(CorrectiveActionStatus status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
}
