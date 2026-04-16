package com.rescuehub.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "access_list_entry")
public class AccessListEntry {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Null means this entry applies globally (all orgs). */
    private Long organizationId;

    /** ALLOW or DENY */
    @Column(nullable = false, length = 10)
    private String listType;

    /** IP, USERNAME, or WORKSTATION_ID */
    @Column(nullable = false, length = 20)
    private String subjectType;

    @Column(nullable = false, length = 512)
    private String subjectValue;

    @Column(length = 512)
    private String reason;

    private Long createdByUserId;

    @Column(nullable = false)
    private Instant createdAt;

    /** Null = never expires. */
    private Instant expiresAt;

    @PrePersist protected void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public String getListType() { return listType; }
    public void setListType(String listType) { this.listType = listType; }
    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String subjectType) { this.subjectType = subjectType; }
    public String getSubjectValue() { return subjectValue; }
    public void setSubjectValue(String subjectValue) { this.subjectValue = subjectValue; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Long getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(Long createdByUserId) { this.createdByUserId = createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
