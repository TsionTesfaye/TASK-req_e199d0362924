package com.rescuehub;

import com.rescuehub.entity.RankedContentEntry;
import com.rescuehub.exception.BusinessRuleException;
import com.rescuehub.exception.ForbiddenException;
import com.rescuehub.service.RankingService;
import com.rescuehub.service.RankingService.Weights;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

class RankingServiceTest extends BaseIntegrationTest {

    @Autowired
    private RankingService rankingService;

    @Test
    @Transactional
    void getWeights_returnsNonNullWeightsWithPositiveValues() {
        Weights w = rankingService.getWeights(adminUser);

        assertNotNull(w);
        // All weights must be non-negative (either defaults or previously set values)
        assertTrue(w.recency() >= 0, "recency must be non-negative");
        assertTrue(w.favorites() >= 0, "favorites must be non-negative");
        assertTrue(w.comments() >= 0, "comments must be non-negative");
        assertTrue(w.moderatorBoost() >= 0, "moderatorBoost must be non-negative");
        assertTrue(w.coldStartBase() >= 0, "coldStartBase must be non-negative");
    }

    @Test
    @Transactional
    void setWeights_adminSetsWeights_persistsAndReturns() {
        Weights input = new Weights(2.0, 3.0, 1.0, 4.0, 0.8);

        Weights result = rankingService.setWeights(adminUser, input, "127.0.0.1", "ws");

        assertNotNull(result);
        assertEquals(2.0, result.recency(), 0.001);
        assertEquals(3.0, result.favorites(), 0.001);
        assertEquals(1.0, result.comments(), 0.001);
        assertEquals(4.0, result.moderatorBoost(), 0.001);
        assertEquals(0.8, result.coldStartBase(), 0.001);

        // Re-fetch: should return the updated values from cache/DB
        Weights fetched = rankingService.getWeights(adminUser);
        assertEquals(2.0, fetched.recency(), 0.001);
    }

    @Test
    @Transactional
    void setWeights_clinicianThrowsForbidden() {
        Weights input = new Weights(1.0, 2.0, 1.5, 5.0, 0.5);

        assertThrows(ForbiddenException.class,
                () -> rankingService.setWeights(clinicianUser, input, "127.0.0.1", "ws"));
    }

    @Test
    @Transactional
    void setWeights_negativeWeightThrowsBusinessRuleException() {
        Weights invalid = new Weights(-1.0, 2.0, 1.5, 5.0, 0.5);

        assertThrows(BusinessRuleException.class,
                () -> rankingService.setWeights(adminUser, invalid, "127.0.0.1", "ws"));
    }

    @Test
    @Transactional
    void promote_adminPromotesIncidentContent_returnsEntry() {
        long nanos = System.nanoTime();

        RankedContentEntry entry = rankingService.promote(
                adminUser, "incident", nanos,
                5, 3, 24L,
                "127.0.0.1", "ws");

        assertNotNull(entry);
        assertNotNull(entry.getId());
        assertEquals("incident", entry.getContentType());
        assertEquals(nanos, entry.getContentId());
        assertEquals(adminUser.getOrganizationId(), entry.getOrganizationId());
        assertNotNull(entry.getScore());
        assertTrue(entry.getScore().doubleValue() > 0);
    }

    @Test
    @Transactional
    void promote_clinicianThrowsForbidden() {
        assertThrows(ForbiddenException.class,
                () -> rankingService.promote(
                        clinicianUser, "incident", 1L,
                        0, 0, 0L,
                        "127.0.0.1", "ws"));
    }

    @Test
    @Transactional
    void list_returnsRankedEntries() {
        long nanos = System.nanoTime();
        rankingService.promote(adminUser, "bulletin", nanos, 2, 1, 12L, "127.0.0.1", "ws");

        Page<RankedContentEntry> page = rankingService.list(adminUser, PageRequest.of(0, 20));

        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 1);
        page.getContent().forEach(e ->
                assertEquals(adminUser.getOrganizationId(), e.getOrganizationId()));
    }

    @Test
    @Transactional
    void syncScore_updatesScoreForContent() {
        long nanos = System.nanoTime();
        // Create initial entry via promote
        rankingService.promote(adminUser, "incident", nanos, 0, 0, 0L, "127.0.0.1", "ws");

        // syncScore should not throw and should upsert the entry
        assertDoesNotThrow(() ->
                rankingService.syncScore(adminUser.getOrganizationId(), "incident", nanos));
    }
}
