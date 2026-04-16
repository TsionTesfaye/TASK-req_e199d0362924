package com.rescuehub.entity;

import com.rescuehub.enums.VisitStatus;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "visit")
public class Visit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long organizationId;
    @Column(nullable = false) private Long patientId;
    private Long appointmentId;
    @Column(nullable = false) private Long createdByUserId;
    private Long clinicianUserId;
    @Column(nullable = false) private Instant openedAt;
    private Instant closedAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private VisitStatus status = VisitStatus.OPEN;
    @Column(columnDefinition = "TEXT") private String chiefComplaint;
    @Column(columnDefinition = "LONGTEXT") private String summaryText;
    @Column(columnDefinition = "TEXT") private String diagnosisText;
    @Column(nullable = false) private boolean qcBlocked = false;
    @Column(nullable = false) private boolean qcOverrideRequired = false;
    private Instant archivedAt;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    @Version private Long version;

    @PrePersist protected void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
    public Long getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(Long createdByUserId) { this.createdByUserId = createdByUserId; }
    public Long getClinicianUserId() { return clinicianUserId; }
    public void setClinicianUserId(Long clinicianUserId) { this.clinicianUserId = clinicianUserId; }
    public Instant getOpenedAt() { return openedAt; }
    public void setOpenedAt(Instant openedAt) { this.openedAt = openedAt; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
    public VisitStatus getStatus() { return status; }
    public void setStatus(VisitStatus status) { this.status = status; }
    public String getChiefComplaint() { return chiefComplaint; }
    public void setChiefComplaint(String chiefComplaint) { this.chiefComplaint = chiefComplaint; }
    public String getSummaryText() { return summaryText; }
    public void setSummaryText(String summaryText) { this.summaryText = summaryText; }
    public String getDiagnosisText() { return diagnosisText; }
    public void setDiagnosisText(String diagnosisText) { this.diagnosisText = diagnosisText; }
    public boolean isQcBlocked() { return qcBlocked; }
    public void setQcBlocked(boolean qcBlocked) { this.qcBlocked = qcBlocked; }
    public boolean isQcOverrideRequired() { return qcOverrideRequired; }
    public void setQcOverrideRequired(boolean qcOverrideRequired) { this.qcOverrideRequired = qcOverrideRequired; }
    public Instant getArchivedAt() { return archivedAt; }
    public void setArchivedAt(Instant archivedAt) { this.archivedAt = archivedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
