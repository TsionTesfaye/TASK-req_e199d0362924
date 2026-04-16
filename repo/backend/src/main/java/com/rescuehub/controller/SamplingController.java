package com.rescuehub.controller;

import com.rescuehub.entity.User;
import com.rescuehub.security.SecurityUtils;
import com.rescuehub.service.SamplingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/sampling")
public class SamplingController {

    private final SamplingService samplingService;

    public SamplingController(SamplingService samplingService) {
        this.samplingService = samplingService;
    }

    record CreateRunRequest(@NotBlank String period, int percentage) {}

    @PostMapping("/runs")
    public Map<String, Object> createRun(@Valid @RequestBody CreateRunRequest req, HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(samplingService.createRun(actor, req.period(),
                req.percentage() > 0 ? req.percentage() : 5,
                request.getRemoteAddr(), SecurityUtils.currentWorkstationId()));
    }

    @GetMapping("/runs")
    public Map<String, Object> listRuns(@RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        User actor = SecurityUtils.currentUser();
        var runs = samplingService.list(actor, PageBounds.of(page, size));
        return ApiResponse.list(runs.getContent(), runs.getTotalElements(), page, size);
    }

    @GetMapping("/runs/{id}/visits")
    public Map<String, Object> getSampledVisits(@PathVariable Long id) {
        User actor = SecurityUtils.currentUser();
        var visits = samplingService.getSampledVisits(actor, id);
        return ApiResponse.list(visits, visits.size(), 0, visits.size());
    }
}
