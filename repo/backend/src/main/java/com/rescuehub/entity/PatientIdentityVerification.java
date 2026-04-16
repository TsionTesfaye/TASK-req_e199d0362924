package com.rescuehub.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "patient_identity_verification")
public class PatientIdentityVerification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long patientId;
    @Column(nullable = false) private Long verifiedByUserId;
    @Column(nullable = false) private String documentType;
    @Column(nullable = false) private String documentLast4;
    @Column(nullable = false) private Instant verifiedAt;
    private String note;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getVerifiedByUserId() { return verifiedByUserId; }
    public void setVerifiedByUserId(Long verifiedByUserId) { this.verifiedByUserId = verifiedByUserId; }
    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }
    public String getDocumentLast4() { return documentLast4; }
    public void setDocumentLast4(String documentLast4) { this.documentLast4 = documentLast4; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
