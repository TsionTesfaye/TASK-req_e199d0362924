package com.rescuehub.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RiskScoreService — EPHEMERAL, NON-ENFORCING risk telemetry.
 *
 * Semantics (explicit, not silent):
 *   - Scores are held only in process memory and are reset whenever the
 *     backend process restarts (container restart, scheduled deploy, crash).
 *   - Scores are NEVER read as an authorization input. No service decision
 *     (login, export, reveal, refund, void) is gated on the score value.
 *     The hard enforcement points (role checks, rate limits, idempotency,
 *     export elevation gates) live in their respective services and remain
 *     authoritative independent of this class.
 *   - This class records events for operator observability only. Persistent
 *     evidence of those events lives in AuditLog (immutable, database-backed).
 *
 * If persistence of risk scores is later required, add a RiskEvent/RiskScore
 * entity + repository and wire enforcement explicitly in the relevant
 * services; do not retrofit silent enforcement here.
 */
@Service
public class RiskScoreService {

    private final Map<String, AtomicInteger> scores = new ConcurrentHashMap<>();

    public void recordEvent(String workstationId, String eventType) {
        int weight = switch (eventType) {
            case "ANOMALOUS_LOGIN" -> 10;
            case "REPEATED_EXPORT" -> 5;
            case "HIGH_VISIT_FREQUENCY" -> 3;
            default -> 1;
        };
        scores.computeIfAbsent(workstationId, k -> new AtomicInteger(0))
              .addAndGet(weight);
    }

    public int getScore(String workstationId) {
        AtomicInteger score = scores.get(workstationId);
        return score != null ? score.get() : 0;
    }

    public Map<String, Integer> getAllScores() {
        Map<String, Integer> result = new java.util.HashMap<>();
        scores.forEach((k, v) -> result.put(k, v.get()));
        return result;
    }

    public void resetScore(String workstationId) {
        scores.remove(workstationId);
    }

    /** Explicitly documents that this store is ephemeral. Used by admin UI / tests. */
    public boolean isEphemeral() {
        return true;
    }
}
