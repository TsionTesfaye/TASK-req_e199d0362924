package com.rescuehub.repository;

import com.rescuehub.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByOrganizationIdAndId(Long orgId, Long id);
    Page<Patient> findByOrganizationId(Long orgId, Pageable pageable);
    boolean existsByOrganizationIdAndMedicalRecordNumber(Long orgId, String mrn);

    @Query("SELECT p FROM Patient p WHERE p.organizationId = :orgId AND p.archivedAt IS NULL AND p.updatedAt < :threshold")
    List<Patient> findActiveOlderThan(Long orgId, Instant threshold);

    /**
     * Filtered list with optional text search (MRN or sex) and archived state.
     * archived=null → active only; archived=true → archived only; archived=false → active only.
     */
    @Query("SELECT p FROM Patient p WHERE p.organizationId = :orgId " +
           "AND (:q IS NULL OR LOWER(p.medicalRecordNumber) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "     OR LOWER(p.sex) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "AND (:showArchived = true OR p.archivedAt IS NULL) " +
           "AND (:archivedOnly = false OR p.archivedAt IS NOT NULL)")
    Page<Patient> findFiltered(Long orgId, String q, boolean showArchived, boolean archivedOnly, Pageable pageable);
}
