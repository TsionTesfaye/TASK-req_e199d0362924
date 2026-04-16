package com.rescuehub.service;

import com.rescuehub.entity.Bulletin;
import com.rescuehub.entity.IncidentReport;
import com.rescuehub.entity.User;
import com.rescuehub.enums.BulletinStatus;
import com.rescuehub.enums.IncidentStatus;
import com.rescuehub.repository.BulletinRepository;
import com.rescuehub.repository.IncidentReportRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class SearchService {

    private final IncidentReportRepository incidentRepo;
    private final BulletinRepository bulletinRepo;

    public SearchService(IncidentReportRepository incidentRepo, BulletinRepository bulletinRepo) {
        this.incidentRepo = incidentRepo;
        this.bulletinRepo = bulletinRepo;
    }

    public record SearchResult(String contentType, Object item, double score) {}

    @Transactional(readOnly = true)
    public List<SearchResult> search(User actor, String q, String contentType, String sort, int page, int size) {
        String query = q != null ? q.toLowerCase() : "";

        // Choose DB-level sort to pre-order candidates matching the requested mode
        Sort dbSort = switch (sort == null ? "" : sort.toLowerCase()) {
            case "popular"   -> Sort.by(Sort.Direction.DESC, "viewCount");
            case "favorites" -> Sort.by(Sort.Direction.DESC, "favoriteCount");
            case "comments"  -> Sort.by(Sort.Direction.DESC, "commentCount");
            default          -> Sort.by(Sort.Direction.DESC, "createdAt"); // "recent" and default
        };
        Pageable pageable = PageRequest.of(page, size, dbSort);

        List<SearchResult> results = new ArrayList<>();

        List<IncidentStatus> excludedIncident = List.of(IncidentStatus.HIDDEN, IncidentStatus.ARCHIVED);
        List<BulletinStatus> excludedBulletin = List.of(BulletinStatus.HIDDEN, BulletinStatus.ARCHIVED);

        if (contentType == null || "incident".equalsIgnoreCase(contentType)) {
            Page<IncidentReport> incidents = incidentRepo.searchVisible(actor.getOrganizationId(), query, excludedIncident, pageable);
            for (IncidentReport i : incidents) {
                double score = computeScore(i.getFavoriteCount(), i.getCommentCount(), i.getViewCount(), i.getCreatedAt().toEpochMilli());
                results.add(new SearchResult("incident", i, score));
            }
        }

        if (contentType == null || "bulletin".equalsIgnoreCase(contentType)) {
            Page<Bulletin> bulletins = bulletinRepo.searchVisible(actor.getOrganizationId(), query, excludedBulletin, pageable);
            for (Bulletin b : bulletins) {
                double score = computeScore(b.getFavoriteCount(), b.getCommentCount(), b.getViewCount(), b.getCreatedAt().toEpochMilli());
                results.add(new SearchResult("bulletin", b, score));
            }
        }

        // Apply the requested sort mode in-memory across the merged incident + bulletin results
        if (sort != null && !sort.isBlank()) {
            results.sort((a, b) -> Long.compare(extractSortKey(b.item(), sort), extractSortKey(a.item(), sort)));
        } else {
            results.sort((a, b) -> Double.compare(b.score(), a.score()));
        }
        return results;
    }

    /** Extract the numeric sort key for a given sort mode from an IncidentReport or Bulletin. */
    private static long extractSortKey(Object item, String sort) {
        return switch (sort.toLowerCase()) {
            case "recent" -> {
                if (item instanceof IncidentReport i) yield i.getCreatedAt().toEpochMilli();
                if (item instanceof Bulletin b) yield b.getCreatedAt().toEpochMilli();
                yield 0L;
            }
            case "popular" -> {
                if (item instanceof IncidentReport i) yield i.getViewCount();
                if (item instanceof Bulletin b) yield b.getViewCount();
                yield 0L;
            }
            case "favorites" -> {
                if (item instanceof IncidentReport i) yield i.getFavoriteCount();
                if (item instanceof Bulletin b) yield b.getFavoriteCount();
                yield 0L;
            }
            case "comments" -> {
                if (item instanceof IncidentReport i) yield i.getCommentCount();
                if (item instanceof Bulletin b) yield b.getCommentCount();
                yield 0L;
            }
            default -> 0L;
        };
    }

    private double computeScore(long favorites, long comments, long views, long createdMillis) {
        double wRecency = 0.4;
        double wFav = 0.3;
        double wComment = 0.2;
        double wView = 0.1;
        double coldStartBase = 10.0;

        long ageMs = System.currentTimeMillis() - createdMillis;
        double freshness = Math.max(0, 1.0 - (ageMs / (7.0 * 24 * 3600 * 1000)));

        return wRecency * freshness * 100 + wFav * favorites + wComment * comments + wView * views + coldStartBase;
    }
}
