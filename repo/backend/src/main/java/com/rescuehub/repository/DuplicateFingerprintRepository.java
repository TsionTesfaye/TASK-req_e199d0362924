package com.rescuehub.repository;

import com.rescuehub.entity.DuplicateFingerprint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DuplicateFingerprintRepository extends JpaRepository<DuplicateFingerprint, Long> {
    Optional<DuplicateFingerprint> findByOrganizationIdAndFingerprintTypeAndFingerprintValue(Long orgId, String type, String value);
    List<DuplicateFingerprint> findByOrganizationIdAndFingerprintTypeAndFingerprintValueAndObjectType(Long orgId, String type, String value, String objectType);
}
