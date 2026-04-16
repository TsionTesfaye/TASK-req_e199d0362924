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
@RequestMapping("/api/comments")
public class CommentController {

    private final FavoriteCommentService service;

    public CommentController(FavoriteCommentService service) {
        this.service = service;
    }

    record CommentRequest(@NotBlank String contentType, @NotNull Long contentId, @NotBlank String body) {}

    @PostMapping
    public Map<String, Object> comment(@Valid @RequestBody CommentRequest req) {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(service.comment(actor, req.contentType(), req.contentId(), req.body()));
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam String contentType, @RequestParam Long contentId,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        User actor = SecurityUtils.currentUser();
        var comments = service.listComments(actor, contentType, contentId, PageBounds.of(page, size));
        return ApiResponse.list(comments.getContent(), comments.getTotalElements(), page, size);
    }
}
