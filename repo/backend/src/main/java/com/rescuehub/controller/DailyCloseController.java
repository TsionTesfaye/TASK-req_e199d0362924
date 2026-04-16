package com.rescuehub.controller;

import com.rescuehub.entity.User;
import com.rescuehub.security.SecurityUtils;
import com.rescuehub.service.DailyCloseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/daily-close")
public class DailyCloseController {

    private final DailyCloseService dailyCloseService;

    public DailyCloseController(DailyCloseService dailyCloseService) {
        this.dailyCloseService = dailyCloseService;
    }

    record CloseRequest(@NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate businessDate) {}

    @PostMapping
    public Map<String, Object> close(@Valid @RequestBody CloseRequest req, HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(dailyCloseService.close(actor, req.businessDate(),
                request.getRemoteAddr(), SecurityUtils.currentWorkstationId()));
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        User actor = SecurityUtils.currentUser();
        var closes = dailyCloseService.list(actor, PageBounds.of(page, size));
        return ApiResponse.list(closes.getContent(), closes.getTotalElements(), page, size);
    }
}
