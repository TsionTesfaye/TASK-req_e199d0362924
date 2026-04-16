package com.rescuehub.service;

import com.rescuehub.entity.IdempotencyKey;
import com.rescuehub.exception.ConflictException;
import com.rescuehub.repository.IdempotencyKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Optional;

@Service
public class IdempotencyService {

    private final IdempotencyKeyRepository repo;

    public IdempotencyService(IdempotencyKeyRepository repo) {
        this.repo = repo;
    }

    /**
     * Returns existing snapshot if key already exists, otherwise returns null (caller should proceed).
     */
    @Transactional
    public String checkAndReserve(Long orgId, Long userId, String key, String requestHash) {
        Optional<IdempotencyKey> existing = repo.findByOrganizationIdAndKey(orgId, key);
        if (existing.isPresent()) {
            IdempotencyKey ik = existing.get();
            if (ik.getResponseSnapshotJson() != null) {
                return ik.getResponseSnapshotJson();
            }
            // key reserved but no response yet — duplicate in-flight
            throw new ConflictException("Idempotency key already in use: " + key);
        }
        IdempotencyKey ik = new IdempotencyKey();
        ik.setOrganizationId(orgId);
        ik.setUserId(userId);
        ik.setKey(key);
        ik.setRequestHash(requestHash);
        ik.setExpiresAt(Instant.now().plusSeconds(86400)); // 24h
        repo.save(ik);
        return null;
    }

    @Transactional
    public void complete(Long orgId, String key, String responseSnapshot) {
        repo.findByOrganizationIdAndKey(orgId, key).ifPresent(ik -> {
            ik.setResponseSnapshotJson(responseSnapshot);
            repo.save(ik);
        });
    }

    @Transactional
    public void cleanup() {
        repo.deleteExpired(Instant.now());
    }
}
