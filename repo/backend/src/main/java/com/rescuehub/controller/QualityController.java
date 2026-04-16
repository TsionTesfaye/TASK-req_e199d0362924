package com.rescuehub.controller;

import com.rescuehub.entity.User;
import com.rescuehub.enums.CorrectiveActionStatus;
import com.rescuehub.security.SecurityUtils;
import com.rescuehub.service.CorrectiveActionService;
import com.rescuehub.service.QualityRulesService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/quality")
public class QualityController {

    private final QualityRulesService qualityService;
    private final CorrectiveActionService correctiveActionService;

    public QualityController(QualityRulesService qualityService, CorrectiveActionService correctiveActionService) {
        this.qualityService = qualityService;
        this.correctiveActionService = correctiveActionService;
    }

    record OverrideRequest(@NotBlank String reasonCode, @NotBlank String note) {}
    record CreateCARequest(@NotBlank String description, Long relatedVisitId, Long relatedRuleResultId) {}
    record TransitionCARequest(@NotNull CorrectiveActionStatus status, String resolutionNote, Long assignedTo) {}

    @GetMapping("/results")
    public Map<String, Object> listResults(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        User actor = SecurityUtils.currentUser();
        var results = qualityService.listResults(actor, PageBounds.of(page, size));
        return ApiResponse.list(results.getContent(), results.getTotalElements(), page, size);
    }

    @GetMapping("/results/{id}")
    public Map<String, Object> getResult(@PathVariable Long id) {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(qualityService.getResult(actor, id));
    }

    @PostMapping("/results/{id}/override")
    public Map<String, Object> override(@PathVariable Long id, @Valid @RequestBody OverrideRequest req,
                                         HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(qualityService.override(actor, id, req.reasonCode(), req.note(),
                request.getRemoteAddr(), SecurityUtils.currentWorkstationId()));
    }

    @PostMapping("/corrective-actions")
    public Map<String, Object> createCA(@Valid @RequestBody CreateCARequest req, HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(correctiveActionService.create(actor, req.description(), req.relatedVisitId(),
                req.relatedRuleResultId(), request.getRemoteAddr(), SecurityUtils.currentWorkstationId()));
    }

    @GetMapping("/corrective-actions")
    public Map<String, Object> listCA(@RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        User actor = SecurityUtils.currentUser();
        var actions = correctiveActionService.list(actor, PageBounds.of(page, size));
        return ApiResponse.list(actions.getContent(), actions.getTotalElements(), page, size);
    }

    @GetMapping("/corrective-actions/{id}")
    public Map<String, Object> getCA(@PathVariable Long id) {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(correctiveActionService.getById(actor, id));
    }

    @PutMapping("/corrective-actions/{id}")
    public Map<String, Object> transitionCA(@PathVariable Long id, @Valid @RequestBody TransitionCARequest req,
                                             HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(correctiveActionService.transition(actor, id, req.status(), req.resolutionNote(),
                req.assignedTo(), request.getRemoteAddr(), SecurityUtils.currentWorkstationId()));
    }
}
