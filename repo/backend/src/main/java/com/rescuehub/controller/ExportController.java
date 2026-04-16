package com.rescuehub.controller;

import com.rescuehub.entity.User;
import com.rescuehub.security.SecurityUtils;
import com.rescuehub.service.ExportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/exports")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    record ExportRequest(@NotBlank String exportType, @NotBlank String idempotencyKey,
                          boolean elevated, boolean secondConfirmation) {}

    @PostMapping
    public Map<String, Object> export(@Valid @RequestBody ExportRequest req, HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        String result = exportService.export(actor, req.exportType(), req.idempotencyKey(),
                req.elevated(), req.secondConfirmation(), request.getRemoteAddr(), SecurityUtils.currentWorkstationId());
        return ApiResponse.data(ApiResponse.safeMap("result", result));
    }

    @GetMapping("/history")
    public Map<String, Object> history(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        User actor = SecurityUtils.currentUser();
        var history = exportService.listHistory(actor, PageRequest.of(page, size));
        return ApiResponse.list(history, history.size(), page, size);
    }
}
