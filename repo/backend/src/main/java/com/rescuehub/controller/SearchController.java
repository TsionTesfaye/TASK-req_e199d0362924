package com.rescuehub.controller;

import com.rescuehub.entity.User;
import com.rescuehub.security.SecurityUtils;
import com.rescuehub.service.SearchService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public Map<String, Object> search(@RequestParam(required = false) String q,
                                       @RequestParam(required = false) String type,
                                       @RequestParam(required = false) String sort,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        User actor = SecurityUtils.currentUser();
        List<SearchService.SearchResult> results = searchService.search(actor, q, type, sort, page, size);
        return ApiResponse.list(results, results.size(), page, size);
    }
}
