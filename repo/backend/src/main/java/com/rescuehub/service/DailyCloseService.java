package com.rescuehub.service;

import com.rescuehub.entity.DailyClose;
import com.rescuehub.entity.User;
import com.rescuehub.enums.DailyCloseStatus;
import com.rescuehub.enums.Role;
import com.rescuehub.exception.BusinessRuleException;
import com.rescuehub.repository.DailyCloseRepository;
import com.rescuehub.security.RoleGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

@Service
public class DailyCloseService {

    private final DailyCloseRepository dailyCloseRepo;
    private final RoleGuard roleGuard;
    private final AuditService auditService;

    public DailyCloseService(DailyCloseRepository dailyCloseRepo, RoleGuard roleGuard, AuditService auditService) {
        this.dailyCloseRepo = dailyCloseRepo;
        this.roleGuard = roleGuard;
        this.auditService = auditService;
    }

    @Transactional
    public DailyClose close(User actor, LocalDate businessDate, String ip, String workstationId) {
        roleGuard.require(actor, Role.BILLING, Role.ADMIN);

        if (dailyCloseRepo.existsByOrganizationIdAndBusinessDate(actor.getOrganizationId(), businessDate)) {
            throw new BusinessRuleException("Daily close already exists for " + businessDate);
        }

        DailyClose dc = new DailyClose();
        dc.setOrganizationId(actor.getOrganizationId());
        dc.setBusinessDate(businessDate);
        dc.setClosedByUserId(actor.getId());
        dc.setClosedAt(Instant.now());
        dc.setStatus(DailyCloseStatus.CLOSED);
        dc = dailyCloseRepo.save(dc);

        auditService.log(actor.getId(), actor.getUsername(), "DAILY_CLOSE",
                "DailyClose", String.valueOf(dc.getId()), actor.getOrganizationId(), ip, workstationId,
                null, "{\"businessDate\":\"" + businessDate + "\"}");

        return dc;
    }

    @Transactional(readOnly = true)
    public Page<DailyClose> list(User actor, Pageable pageable) {
        roleGuard.require(actor, Role.BILLING, Role.ADMIN);
        return dailyCloseRepo.findByOrganizationId(actor.getOrganizationId(), pageable);
    }
}
