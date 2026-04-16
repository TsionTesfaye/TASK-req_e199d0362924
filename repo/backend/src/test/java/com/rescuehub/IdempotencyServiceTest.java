package com.rescuehub;

import com.rescuehub.exception.ConflictException;
import com.rescuehub.service.IdempotencyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class IdempotencyServiceTest extends BaseIntegrationTest {

    @Autowired
    private IdempotencyService idempotencyService;

    @Test
    @Transactional
    void firstCallReturnsNull() {
        String key = "test-key-" + UUID.randomUUID();
        String cached = idempotencyService.checkAndReserve(testOrg.getId(), adminUser.getId(), key, "hash1");
        assertNull(cached, "First call should return null (proceed)");
    }

    @Test
    void secondCallReturnsCachedResponse() {
        String key = "test-key-" + UUID.randomUUID();
        idempotencyService.checkAndReserve(testOrg.getId(), adminUser.getId(), key, "hash1");
        idempotencyService.complete(testOrg.getId(), key, "{\"result\":\"done\"}");

        String cached = idempotencyService.checkAndReserve(testOrg.getId(), adminUser.getId(), key, "hash1");
        assertNotNull(cached);
        assertTrue(cached.contains("done"), "Should return cached response snapshot");
    }

    @Test
    void inFlightKeyThrowsConflict() {
        String key = "inflight-key-" + UUID.randomUUID();
        idempotencyService.checkAndReserve(testOrg.getId(), adminUser.getId(), key, "hash1");
        // Key reserved but not completed — second call should throw
        assertThrows(ConflictException.class, () ->
                idempotencyService.checkAndReserve(testOrg.getId(), adminUser.getId(), key, "hash1"));
    }

    @Test
    void cleanup_doesNotThrow() {
        // Insert and complete a key, then run cleanup — should not throw regardless of expiry state
        String key = "cleanup-key-" + UUID.randomUUID();
        idempotencyService.checkAndReserve(testOrg.getId(), adminUser.getId(), key, "hash-cleanup");
        idempotencyService.complete(testOrg.getId(), key, "{\"result\":\"ok\"}");
        assertDoesNotThrow(() -> idempotencyService.cleanup());
    }
}
