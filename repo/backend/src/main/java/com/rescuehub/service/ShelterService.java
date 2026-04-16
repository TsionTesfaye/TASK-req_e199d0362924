package com.rescuehub.service;

import com.rescuehub.entity.ShelterResource;
import com.rescuehub.entity.User;
import com.rescuehub.enums.Role;
import com.rescuehub.exception.NotFoundException;
import com.rescuehub.repository.ShelterResourceRepository;
import com.rescuehub.security.RoleGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ShelterService {

    private final ShelterResourceRepository repo;
    private final AuditService auditService;
    private final RoleGuard roleGuard;

    public ShelterService(ShelterResourceRepository repo, AuditService auditService, RoleGuard roleGuard) {
        this.repo = repo;
        this.auditService = auditService;
        this.roleGuard = roleGuard;
    }

    @Transactional
    public ShelterResource create(User actor, String name, String category, String neighborhood,
                                   String addressText, BigDecimal lat, BigDecimal lon,
                                   String ip, String workstationId) {
        roleGuard.require(actor, Role.ADMIN);
        ShelterResource sr = new ShelterResource();
        sr.setOrganizationId(actor.getOrganizationId());
        sr.setName(name);
        sr.setCategory(category);
        sr.setNeighborhood(neighborhood);
        sr.setAddressText(addressText);
        sr.setLatitude(lat);
        sr.setLongitude(lon);
        sr.setActive(true);
        sr = repo.save(sr);
        auditService.log(actor.getId(), actor.getUsername(), "SHELTER_CREATE",
                "ShelterResource", String.valueOf(sr.getId()), actor.getOrganizationId(), ip, workstationId, null, null);
        return sr;
    }

    @Transactional(readOnly = true)
    public ShelterResource getById(User actor, Long id) {
        roleGuard.require(actor, Role.FRONT_DESK, Role.CLINICIAN, Role.ADMIN);
        return repo.findByOrganizationIdAndId(actor.getOrganizationId(), id)
                .orElseThrow(() -> new NotFoundException("Shelter not found"));
    }

    @Transactional(readOnly = true)
    public Page<ShelterResource> list(User actor, Pageable pageable) {
        roleGuard.require(actor, Role.FRONT_DESK, Role.CLINICIAN, Role.ADMIN);
        return repo.findByOrganizationId(actor.getOrganizationId(), pageable);
    }

    @Transactional
    public ShelterResource update(User actor, Long id, String name, String category, String neighborhood,
                                   String addressText, BigDecimal lat, BigDecimal lon, boolean isActive,
                                   String ip, String workstationId) {
        roleGuard.require(actor, Role.ADMIN);
        ShelterResource sr = getById(actor, id);
        sr.setName(name);
        sr.setCategory(category);
        sr.setNeighborhood(neighborhood);
        sr.setAddressText(addressText);
        sr.setLatitude(lat);
        sr.setLongitude(lon);
        sr.setActive(isActive);
        sr = repo.save(sr);
        auditService.log(actor.getId(), actor.getUsername(), "SHELTER_UPDATE",
                "ShelterResource", String.valueOf(id), actor.getOrganizationId(), ip, workstationId, null, null);
        return sr;
    }
}
