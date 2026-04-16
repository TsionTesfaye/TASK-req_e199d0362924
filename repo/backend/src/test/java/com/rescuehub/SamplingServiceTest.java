package com.rescuehub;

import com.rescuehub.entity.Organization;
import com.rescuehub.entity.SampledVisit;
import com.rescuehub.entity.SamplingRun;
import com.rescuehub.entity.User;
import com.rescuehub.enums.Role;
import com.rescuehub.exception.ForbiddenException;
import com.rescuehub.repository.SamplingRunRepository;
import com.rescuehub.service.SamplingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class SamplingServiceTest extends BaseIntegrationTest {

    @Autowired
    private SamplingService samplingService;

    @Autowired
    private SamplingRunRepository samplingRunRepo;

    @Test
    void deterministicSelection() {
        String seed = UUID.randomUUID().toString();
        // Same inputs always produce same result
        boolean r1 = SamplingService.selectVisit(42L, seed, 5);
        boolean r2 = SamplingService.selectVisit(42L, seed, 5);
        assertEquals(r1, r2, "Selection should be deterministic");
    }

    @Test
    void differentSeedsProduceDifferentResults() {
        // Not guaranteed for all IDs but very likely to differ
        String seed1 = "seed-aaa";
        String seed2 = "seed-bbb";
        int matches = 0;
        for (long id = 1; id <= 100; id++) {
            if (SamplingService.selectVisit(id, seed1, 50) == SamplingService.selectVisit(id, seed2, 50)) {
                matches++;
            }
        }
        // They shouldn't all match — if 50% random, expect ~50% overlap at most
        assertTrue(matches < 100, "Different seeds should not always produce identical results");
    }

    @Test
    void percentageRespected() {
        String seed = "test-seed-percentage";
        int percentage = 10;
        long selected = 0;
        for (long id = 1; id <= 1000; id++) {
            if (SamplingService.selectVisit(id, seed, percentage)) selected++;
        }
        // Should be approximately 10% — allow ±5%
        assertTrue(selected >= 50 && selected <= 150,
                "Expected ~10% selection but got " + selected + " out of 1000");
    }

    @Test
    void createSamplingRun() {
        String period = "2026-W01-test-" + System.currentTimeMillis();
        var run = samplingService.createRun(adminUser, period, 5, "127.0.0.1", "ws1");
        assertNotNull(run.getId());
        assertEquals(period, run.getPeriod());
        assertEquals(5, run.getPercentage());
        assertNotNull(run.getSeed());
    }

    @Test
    void list_returnsRunsForOrg() {
        String period = "2026-W02-list-" + System.currentTimeMillis();
        samplingService.createRun(qualityUser, period, 10, "127.0.0.1", "ws1");

        Page<SamplingRun> page = samplingService.list(qualityUser, PageRequest.of(0, 20));
        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 1);
    }

    @Test
    void getSampledVisits_returnsListForRun() {
        String period = "2026-W03-sv-" + System.currentTimeMillis();
        SamplingRun run = samplingService.createRun(qualityUser, period, 100, "127.0.0.1", "ws1");

        // May be empty if no closed visits exist, but must not throw
        List<SampledVisit> visits = samplingService.getSampledVisits(qualityUser, run.getId());
        assertNotNull(visits);
    }

    @Test
    void getSampledVisits_unknownRunId_throwsNotFound() {
        assertThrows(com.rescuehub.exception.NotFoundException.class, () ->
                samplingService.getSampledVisits(qualityUser, Long.MAX_VALUE));
    }

    @Test
    void createRun_clinicianForbidden() {
        assertThrows(com.rescuehub.exception.ForbiddenException.class, () ->
                samplingService.createRun(clinicianUser, "2026-W04-fbd-" + System.currentTimeMillis(),
                        5, "127.0.0.1", "ws1"));
    }

    @Test
    void createRun_blankPeriod_throwsBusinessRule() {
        assertThrows(com.rescuehub.exception.BusinessRuleException.class, () ->
                samplingService.createRun(qualityUser, "   ", 10, "127.0.0.1", "ws1"));
    }

    @Test
    void createRun_zeroPct_throwsBusinessRule() {
        assertThrows(com.rescuehub.exception.BusinessRuleException.class, () ->
                samplingService.createRun(qualityUser, "2026-invalid-pct", 0, "127.0.0.1", "ws1"));
    }

    @Test
    void createRun_over100Pct_throwsBusinessRule() {
        assertThrows(com.rescuehub.exception.BusinessRuleException.class, () ->
                samplingService.createRun(qualityUser, "2026-over100-pct", 101, "127.0.0.1", "ws1"));
    }

    @Test
    void createRun_duplicatePeriod_throwsBusinessRule() {
        String period = "2026-dup-" + System.currentTimeMillis();
        samplingService.createRun(qualityUser, period, 10, "127.0.0.1", "ws1");
        assertThrows(com.rescuehub.exception.BusinessRuleException.class, () ->
                samplingService.createRun(qualityUser, period, 20, "127.0.0.1", "ws1"));
    }

    @Test
    void getSampledVisits_crossOrgRun_throwsForbidden() {
        // Create a run in testOrg
        String period = "2026-crossorg-" + System.currentTimeMillis();
        SamplingRun run = samplingService.createRun(qualityUser, period, 5, "127.0.0.1", "ws1");

        // Create a user from a different org
        Organization otherOrg = new Organization();
        otherOrg.setCode("SAMP-OTHER-" + System.nanoTime());
        otherOrg.setName("Other Sampling Org");
        otherOrg.setActive(true);
        otherOrg = orgRepo.save(otherOrg);

        User otherUser = new User();
        otherUser.setOrganizationId(otherOrg.getId());
        otherUser.setUsername("samp_other_" + System.nanoTime());
        otherUser.setPasswordHash("x");
        otherUser.setDisplayName("Other Sampling User");
        otherUser.setRole(Role.QUALITY);
        otherUser.setActive(true);
        otherUser.setFrozen(false);
        otherUser.setPasswordChangedAt(java.time.Instant.now());
        otherUser = userRepo.save(otherUser);

        final User finalOtherUser = otherUser;
        final Long runId = run.getId();
        assertThrows(ForbiddenException.class, () ->
                samplingService.getSampledVisits(finalOtherUser, runId));
    }
}
