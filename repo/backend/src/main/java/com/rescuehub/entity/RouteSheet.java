package com.rescuehub.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "route_sheet")
public class RouteSheet {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long incidentReportId;
    @Column(nullable = false) private Long resourceId;
    @Column(nullable = false) private Long generatedByUserId;
    @Column(nullable = false, columnDefinition = "LONGTEXT") private String routeSummaryText;
    private String printableFilePath;
    @Column(nullable = false) private Instant createdAt;

    @PrePersist protected void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getIncidentReportId() { return incidentReportId; }
    public void setIncidentReportId(Long incidentReportId) { this.incidentReportId = incidentReportId; }
    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
    public Long getGeneratedByUserId() { return generatedByUserId; }
    public void setGeneratedByUserId(Long generatedByUserId) { this.generatedByUserId = generatedByUserId; }
    public String getRouteSummaryText() { return routeSummaryText; }
    public void setRouteSummaryText(String routeSummaryText) { this.routeSummaryText = routeSummaryText; }
    public String getPrintableFilePath() { return printableFilePath; }
    public void setPrintableFilePath(String printableFilePath) { this.printableFilePath = printableFilePath; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
