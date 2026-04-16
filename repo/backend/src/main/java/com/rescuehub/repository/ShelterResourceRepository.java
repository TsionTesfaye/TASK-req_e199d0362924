package com.rescuehub.repository;

import com.rescuehub.entity.ShelterResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ShelterResourceRepository extends JpaRepository<ShelterResource, Long> {
    Optional<ShelterResource> findByOrganizationIdAndId(Long orgId, Long id);
    Page<ShelterResource> findByOrganizationId(Long orgId, Pageable pageable);
    List<ShelterResource> findByOrganizationIdAndIsActive(Long orgId, boolean isActive);
}
