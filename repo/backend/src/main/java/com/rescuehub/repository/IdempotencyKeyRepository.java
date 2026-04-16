package com.rescuehub.repository;

import com.rescuehub.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {
    @org.springframework.data.jpa.repository.Query("SELECT i FROM IdempotencyKey i WHERE i.organizationId = :orgId AND i.key = :key")
    Optional<IdempotencyKey> findByOrganizationIdAndKey(Long orgId, String key);

    @Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("DELETE FROM IdempotencyKey i WHERE i.expiresAt < :now")
    void deleteExpired(Instant now);
}
