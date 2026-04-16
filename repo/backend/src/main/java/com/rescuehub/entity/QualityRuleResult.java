package com.rescuehub.entity;

import com.rescuehub.enums.QualityResultStatus;
import com.rescuehub.enums.QualitySeverity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "quality_rule_result")
public class QualityRuleResult {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long organizationId;
    private Long visitId;
    private Long patientId;
    private Long incidentReportId;
    @Column(nullable = false) private String ruleCode;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private QualitySeverity severity;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private QualityResultStatus status = QualityResultStatus.OPEN;
    @Column(columnDefinition = "JSON") private String resultDetailsJson;
    @Column(nullable = false) private Instant createdAt;
    private Instant resolvedAt;

    @PrePersist protected void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public Long getVisitId() { return visitId; }
    public void setVisitId(Long visitId) { this.visitId = visitId; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getIncidentReportId() { return incidentReportId; }
    public void setIncidentReportId(Long incidentReportId) { this.incidentReportId = incidentReportId; }
    public String getRuleCode() { return ruleCode; }
    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }
    public QualitySeverity getSeverity() { return severity; }
    public void setSeverity(QualitySeverity severity) { this.severity = severity; }
    public QualityResultStatus getStatus() { return status; }
    public void setStatus(QualityResultStatus status) { this.status = status; }
    public String getResultDetailsJson() { return resultDetailsJson; }
    public void setResultDetailsJson(String resultDetailsJson) { this.resultDetailsJson = resultDetailsJson; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
}
