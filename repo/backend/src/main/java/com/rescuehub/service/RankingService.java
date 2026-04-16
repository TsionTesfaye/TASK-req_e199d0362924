package com.rescuehub.service;

import com.rescuehub.entity.OrgRankingWeights;
import com.rescuehub.entity.RankedContentEntry;
import com.rescuehub.entity.User;
import com.rescuehub.enums.Role;
import com.rescuehub.exception.BusinessRuleException;
import com.rescuehub.exception.ForbiddenException;
import com.rescuehub.repository.CommentRepository;
import com.rescuehub.repository.FavoriteRepository;
import com.rescuehub.repository.OrgRankingWeightsRepository;
import com.rescuehub.repository.RankedContentEntryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configurable ranking weights + content promotion (RankedContentEntry persistence).
 *
 * Weights are stored per organization in memory but every promote() write also persists the
 * snapshot used into RankedContentEntry.weighting_snapshot_json so that historical scores are
 * reproducible. Adjusting weights does not retroactively rescore — operators promote items
 * to apply the new weights.
 */
@Service
public class RankingService {

    public record Weights(double recency, double favorites, double comments,
                           double moderatorBoost, double coldStartBase) {
        public static Weights defaults() {
            return new Weights(1.0, 2.0, 1.5, 5.0, 0.5);
        }
    }

    private final RankedContentEntryRepository rankedRepo;
    private final OrgRankingWeightsRepository weightsRepo;
    private final FavoriteRepository favoriteRepo;
    private final CommentRepository commentRepo;
    private final AuditService auditService;
    /** Write-through in-memory cache: avoids a DB read on every promote() or getWeights() call. */
    private final Map<Long, Weights> orgWeightsCache = new ConcurrentHashMap<>();

    public RankingService(RankedContentEntryRepository rankedRepo,
                          OrgRankingWeightsRepository weightsRepo,
                          FavoriteRepository favoriteRepo,
                          CommentRepository commentRepo,
                          AuditService auditService) {
        this.rankedRepo = rankedRepo;
        this.weightsRepo = weightsRepo;
        this.favoriteRepo = favoriteRepo;
        this.commentRepo = commentRepo;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Weights getWeights(User actor) {
        if (actor == null) throw new ForbiddenException("Authentication required");
        // Check in-memory cache first (populated on setWeights or previous getWeights call)
        Weights cached = orgWeightsCache.get(actor.getOrganizationId());
        if (cached != null) return cached;
        // Load from DB; fall back to defaults if never configured
        return weightsRepo.findById(actor.getOrganizationId())
                .map(row -> {
                    Weights w = new Weights(row.getRecency(), row.getFavorites(), row.getComments(),
                            row.getModeratorBoost(), row.getColdStartBase());
                    orgWeightsCache.put(actor.getOrganizationId(), w);
                    return w;
                })
                .orElse(Weights.defaults());
    }

    @Transactional
    public Weights setWeights(User actor, Weights w, String ip, String workstationId) {
        if (actor == null) throw new ForbiddenException("Authentication required");
        if (actor.getRole() != Role.MODERATOR && actor.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Role not permitted to configure ranking weights");
        }
        if (w == null) throw new BusinessRuleException("weights are required");
        if (w.recency < 0 || w.favorites < 0 || w.comments < 0
                || w.moderatorBoost < 0 || w.coldStartBase < 0) {
            throw new BusinessRuleException("weights must be non-negative");
        }

        // Persist to DB (upsert by org PK)
        OrgRankingWeights row = weightsRepo.findById(actor.getOrganizationId())
                .orElseGet(OrgRankingWeights::new);
        row.setOrganizationId(actor.getOrganizationId());
        row.setRecency(w.recency);
        row.setFavorites(w.favorites);
        row.setComments(w.comments);
        row.setModeratorBoost(w.moderatorBoost);
        row.setColdStartBase(w.coldStartBase);
        weightsRepo.save(row);

        // Update in-memory cache
        orgWeightsCache.put(actor.getOrganizationId(), w);

        auditService.log(actor.getId(), actor.getUsername(), "RANKING_WEIGHTS_UPDATED",
                "RankingWeights", String.valueOf(actor.getOrganizationId()),
                actor.getOrganizationId(), ip, workstationId, null,
                "{\"recency\":" + w.recency + ",\"favorites\":" + w.favorites
                        + ",\"comments\":" + w.comments + ",\"moderatorBoost\":" + w.moderatorBoost
                        + ",\"coldStartBase\":" + w.coldStartBase + "}");
        return w;
    }

    @Transactional
    public RankedContentEntry promote(User actor, String contentType, Long contentId,
                                       int favoriteCount, int commentCount, long ageHours,
                                       String ip, String workstationId) {
        if (actor == null) throw new ForbiddenException("Authentication required");
        if (actor.getRole() != Role.MODERATOR && actor.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Role not permitted to promote content");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new BusinessRuleException("contentType is required");
        }
        if (contentId == null) {
            throw new BusinessRuleException("contentId is required");
        }
        Weights w = getWeights(actor);
        double freshness = ageHours <= 0 ? 1.0 : 1.0 / Math.log(2 + ageHours);
        double score = w.recency * freshness
                + w.favorites * favoriteCount
                + w.comments * commentCount
                + w.moderatorBoost
                + w.coldStartBase;

        RankedContentEntry entry = rankedRepo.findByOrganizationIdAndContentTypeAndContentId(
                actor.getOrganizationId(), contentType, contentId).orElseGet(RankedContentEntry::new);
        entry.setOrganizationId(actor.getOrganizationId());
        entry.setContentType(contentType);
        entry.setContentId(contentId);
        entry.setScore(java.math.BigDecimal.valueOf(score).setScale(4, java.math.RoundingMode.HALF_UP));
        entry.setPromotedByUserId(actor.getId());
        entry.setUpdatedAt(Instant.now());
        if (entry.getCreatedAt() == null) entry.setCreatedAt(Instant.now());
        entry.setWeightingSnapshotJson(weightsJson(w, freshness, favoriteCount, commentCount));
        entry = rankedRepo.save(entry);

        auditService.log(actor.getId(), actor.getUsername(), "RANKED_CONTENT_PROMOTED",
                "RankedContentEntry", String.valueOf(entry.getId()),
                actor.getOrganizationId(), ip, workstationId, null,
                "{\"contentType\":\"" + contentType + "\",\"contentId\":" + contentId
                        + ",\"score\":" + score + "}");
        return entry;
    }

    @Transactional(readOnly = true)
    public Page<RankedContentEntry> list(User actor, Pageable pageable) {
        if (actor == null) throw new ForbiddenException("Authentication required");
        return rankedRepo.findByOrganizationIdOrderByScoreDesc(actor.getOrganizationId(), pageable);
    }

    /**
     * Automatically re-scores a content item using live counts from the DB.
     * Called internally when a favorite, unfavorite, comment, or view event occurs.
     * Does NOT require a moderator/admin role — this is system-driven, not operator-driven.
     */
    @Transactional
    public void syncScore(Long orgId, String contentType, Long contentId) {
        long favorites = favoriteRepo.countByContentTypeAndContentId(contentType, contentId);
        long comments = commentRepo.countByContentTypeAndContentId(contentType, contentId);
        Weights w = orgWeightsCache.getOrDefault(orgId, Weights.defaults());
        // Freshness defaults to 1.0 (treat as current content). Operators can override via promote().
        double score = w.recency * 1.0
                + w.favorites * favorites
                + w.comments * comments
                + w.coldStartBase;

        RankedContentEntry entry = rankedRepo
                .findByOrganizationIdAndContentTypeAndContentId(orgId, contentType, contentId)
                .orElseGet(RankedContentEntry::new);
        entry.setOrganizationId(orgId);
        entry.setContentType(contentType);
        entry.setContentId(contentId);
        entry.setScore(java.math.BigDecimal.valueOf(score).setScale(4, java.math.RoundingMode.HALF_UP));
        entry.setUpdatedAt(Instant.now());
        if (entry.getCreatedAt() == null) entry.setCreatedAt(Instant.now());
        entry.setWeightingSnapshotJson(weightsJson(w, 1.0, (int) favorites, (int) comments));
        rankedRepo.save(entry);
    }

    private String weightsJson(Weights w, double freshness, int fav, int com) {
        Map<String, Object> m = new HashMap<>();
        m.put("recency", w.recency);
        m.put("favorites", w.favorites);
        m.put("comments", w.comments);
        m.put("moderatorBoost", w.moderatorBoost);
        m.put("coldStartBase", w.coldStartBase);
        m.put("freshnessFactor", freshness);
        m.put("favoriteCount", fav);
        m.put("commentCount", com);
        // Tiny manual encoder — no Jackson dependency on this hot path
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var e : m.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(e.getKey()).append("\":").append(e.getValue());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}
