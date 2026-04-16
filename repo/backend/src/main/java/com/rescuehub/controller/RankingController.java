package com.rescuehub.controller;

import com.rescuehub.entity.User;
import com.rescuehub.security.SecurityUtils;
import com.rescuehub.service.RankingService;
import com.rescuehub.service.RankingService.Weights;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ranking")
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    record WeightsDto(double recency, double favorites, double comments,
                       double moderatorBoost, double coldStartBase) {
        Weights toService() { return new Weights(recency, favorites, comments, moderatorBoost, coldStartBase); }
    }

    record PromoteRequest(@NotBlank String contentType, @NotNull Long contentId,
                           Integer favoriteCount, Integer commentCount, Long ageHours) {}

    @GetMapping("/weights")
    public Map<String, Object> getWeights() {
        User actor = SecurityUtils.currentUser();
        Weights w = rankingService.getWeights(actor);
        return ApiResponse.data(ApiResponse.safeMap(
                "recency", w.recency(),
                "favorites", w.favorites(),
                "comments", w.comments(),
                "moderatorBoost", w.moderatorBoost(),
                "coldStartBase", w.coldStartBase()));
    }

    @PutMapping("/weights")
    public Map<String, Object> setWeights(@Valid @RequestBody WeightsDto req, HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        Weights w = rankingService.setWeights(actor, req.toService(),
                request.getRemoteAddr(), SecurityUtils.currentWorkstationId());
        return ApiResponse.data(ApiResponse.safeMap(
                "recency", w.recency(),
                "favorites", w.favorites(),
                "comments", w.comments(),
                "moderatorBoost", w.moderatorBoost(),
                "coldStartBase", w.coldStartBase()));
    }

    @PostMapping("/promote")
    public Map<String, Object> promote(@Valid @RequestBody PromoteRequest req, HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(rankingService.promote(actor, req.contentType(), req.contentId(),
                req.favoriteCount() == null ? 0 : req.favoriteCount(),
                req.commentCount() == null ? 0 : req.commentCount(),
                req.ageHours() == null ? 0 : req.ageHours(),
                request.getRemoteAddr(), SecurityUtils.currentWorkstationId()));
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        User actor = SecurityUtils.currentUser();
        var entries = rankingService.list(actor, PageBounds.of(page, size));
        return ApiResponse.list(entries.getContent(), entries.getTotalElements(), page, size);
    }
}
