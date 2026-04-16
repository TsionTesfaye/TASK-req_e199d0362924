package com.rescuehub.controller;

import com.rescuehub.entity.User;
import com.rescuehub.security.SecurityUtils;
import com.rescuehub.service.FavoriteCommentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteCommentService service;

    public FavoriteController(FavoriteCommentService service) {
        this.service = service;
    }

    record FavoriteRequest(@NotBlank String contentType, @NotNull Long contentId) {}

    @PostMapping
    public Map<String, Object> favorite(@Valid @RequestBody FavoriteRequest req) {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(service.favorite(actor, req.contentType(), req.contentId()));
    }

    @DeleteMapping
    public Map<String, Object> unfavorite(@RequestParam String contentType, @RequestParam Long contentId) {
        User actor = SecurityUtils.currentUser();
        service.unfavorite(actor, contentType, contentId);
        return ApiResponse.data(ApiResponse.safeMap("status", "removed"));
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        User actor = SecurityUtils.currentUser();
        var favs = service.listFavorites(actor, PageBounds.of(page, size));
        return ApiResponse.list(favs.getContent(), favs.getTotalElements(), page, size);
    }
}
