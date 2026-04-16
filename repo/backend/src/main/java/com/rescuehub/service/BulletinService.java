package com.rescuehub.service;

import com.rescuehub.entity.Bulletin;
import com.rescuehub.entity.User;
import com.rescuehub.enums.BulletinStatus;
import com.rescuehub.enums.Role;
import com.rescuehub.exception.NotFoundException;
import com.rescuehub.repository.BulletinRepository;
import com.rescuehub.security.RoleGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BulletinService {

    private final BulletinRepository bulletinRepo;
    private final AuditService auditService;
    private final RoleGuard roleGuard;

    public BulletinService(BulletinRepository bulletinRepo, AuditService auditService, RoleGuard roleGuard) {
        this.bulletinRepo = bulletinRepo;
        this.auditService = auditService;
        this.roleGuard = roleGuard;
    }

    @Transactional
    public Bulletin create(User actor, String title, String body, String ip, String workstationId) {
        roleGuard.require(actor, Role.ADMIN, Role.MODERATOR);
        Bulletin b = new Bulletin();
        b.setOrganizationId(actor.getOrganizationId());
        b.setTitle(title);
        b.setBody(body);
        b.setStatus(BulletinStatus.DRAFT);
        b.setCreatedByUserId(actor.getId());
        b = bulletinRepo.save(b);
        auditService.log(actor.getId(), actor.getUsername(), "BULLETIN_CREATE",
                "Bulletin", String.valueOf(b.getId()), actor.getOrganizationId(), ip, workstationId, null, null);
        return b;
    }

    @Transactional
    public Bulletin updateStatus(User actor, Long id, BulletinStatus status, String ip, String workstationId) {
        roleGuard.require(actor, Role.MODERATOR, Role.ADMIN);
        Bulletin b = getById(actor, id);
        String before = "{\"status\":\"" + b.getStatus() + "\"}";
        b.setStatus(status);
        b.setModeratedByUserId(actor.getId());
        b = bulletinRepo.save(b);
        auditService.log(actor.getId(), actor.getUsername(), "BULLETIN_MODERATE",
                "Bulletin", String.valueOf(id), actor.getOrganizationId(), ip, workstationId, before,
                "{\"status\":\"" + status + "\"}");
        return b;
    }

    @Transactional(readOnly = true)
    public Bulletin getById(User actor, Long id) {
        roleGuard.require(actor, Role.FRONT_DESK, Role.CLINICIAN, Role.BILLING, Role.QUALITY, Role.MODERATOR, Role.ADMIN);
        return bulletinRepo.findByOrganizationIdAndId(actor.getOrganizationId(), id)
                .orElseThrow(() -> new NotFoundException("Bulletin not found"));
    }

    @Transactional(readOnly = true)
    public Page<Bulletin> list(User actor, Pageable pageable) {
        roleGuard.require(actor, Role.FRONT_DESK, Role.CLINICIAN, Role.BILLING, Role.QUALITY, Role.MODERATOR, Role.ADMIN);
        return bulletinRepo.findByOrganizationId(actor.getOrganizationId(), pageable);
    }
}
