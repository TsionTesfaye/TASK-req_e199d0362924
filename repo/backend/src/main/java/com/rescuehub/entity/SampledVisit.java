package com.rescuehub.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "sampled_visit")
public class SampledVisit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long samplingRunId;
    @Column(nullable = false) private Long visitId;
    @Column(nullable = false) private String selectionReason;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSamplingRunId() { return samplingRunId; }
    public void setSamplingRunId(Long samplingRunId) { this.samplingRunId = samplingRunId; }
    public Long getVisitId() { return visitId; }
    public void setVisitId(Long visitId) { this.visitId = visitId; }
    public String getSelectionReason() { return selectionReason; }
    public void setSelectionReason(String selectionReason) { this.selectionReason = selectionReason; }
}
