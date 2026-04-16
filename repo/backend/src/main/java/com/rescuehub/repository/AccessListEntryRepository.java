package com.rescuehub.repository;

import com.rescuehub.entity.AccessListEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface AccessListEntryRepository extends JpaRepository<AccessListEntry, Long> {

    /**
     * Find all active entries (not expired) matching a subject for a given org OR global (null org).
     */
    @Query("SELECT e FROM AccessListEntry e WHERE " +
           "(e.organizationId = :orgId OR e.organizationId IS NULL) AND " +
           "e.subjectType = :subjectType AND e.subjectValue = :subjectValue AND " +
           "(e.expiresAt IS NULL OR e.expiresAt > :now)")
    List<AccessListEntry> findActive(Long orgId, String subjectType, String subjectValue, Instant now);

    /** List all entries for a given org (for admin UI). */
    List<AccessListEntry> findByOrganizationId(Long organizationId);
}
