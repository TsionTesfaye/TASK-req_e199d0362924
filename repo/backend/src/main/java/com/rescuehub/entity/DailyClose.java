package com.rescuehub.entity;

import com.rescuehub.enums.DailyCloseStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "daily_close")
public class DailyClose {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long organizationId;
    @Column(nullable = false) private LocalDate businessDate;
    @Column(nullable = false) private Long closedByUserId;
    @Column(nullable = false) private Instant closedAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private DailyCloseStatus status = DailyCloseStatus.CLOSED;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrganizationId() { return organizationId; }
    public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
    public LocalDate getBusinessDate() { return businessDate; }
    public void setBusinessDate(LocalDate businessDate) { this.businessDate = businessDate; }
    public Long getClosedByUserId() { return closedByUserId; }
    public void setClosedByUserId(Long closedByUserId) { this.closedByUserId = closedByUserId; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
    public DailyCloseStatus getStatus() { return status; }
    public void setStatus(DailyCloseStatus status) { this.status = status; }
}
