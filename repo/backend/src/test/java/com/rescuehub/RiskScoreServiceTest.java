package com.rescuehub;

import com.rescuehub.service.RiskScoreService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RiskScoreServiceTest extends BaseIntegrationTest {

    @Autowired
    private RiskScoreService riskScoreService;

    @Test
    void recordEvent_anomalousLogin_incrementsScoreBy10() {
        String ws = "ws-risk-" + System.nanoTime();

        int before = riskScoreService.getScore(ws);
        riskScoreService.recordEvent(ws, "ANOMALOUS_LOGIN");
        int after = riskScoreService.getScore(ws);

        assertEquals(before + 10, after);
    }

    @Test
    void recordEvent_repeatedExport_incrementsScoreBy5() {
        String ws = "ws-export-" + System.nanoTime();

        riskScoreService.recordEvent(ws, "REPEATED_EXPORT");
        assertEquals(5, riskScoreService.getScore(ws));
    }

    @Test
    void getScore_unknownWorkstation_returnsZero() {
        String ws = "ws-unknown-" + System.nanoTime();
        assertEquals(0, riskScoreService.getScore(ws));
    }

    @Test
    void getAllScores_includesRecordedWorkstation() {
        String ws = "ws-all-" + System.nanoTime();
        riskScoreService.recordEvent(ws, "HIGH_VISIT_FREQUENCY");

        Map<String, Integer> all = riskScoreService.getAllScores();
        assertTrue(all.containsKey(ws));
        assertEquals(3, all.get(ws));
    }

    @Test
    void resetScore_removesWorkstation() {
        String ws = "ws-reset-" + System.nanoTime();
        riskScoreService.recordEvent(ws, "ANOMALOUS_LOGIN");
        assertTrue(riskScoreService.getScore(ws) > 0);

        riskScoreService.resetScore(ws);

        assertEquals(0, riskScoreService.getScore(ws));
    }

    @Test
    void isEphemeral_returnsTrue() {
        assertTrue(riskScoreService.isEphemeral());
    }
}
