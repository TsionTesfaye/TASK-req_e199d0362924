package com.rescuehub.controller;

import com.rescuehub.entity.User;
import com.rescuehub.enums.Role;
import com.rescuehub.security.RoleGuard;
import com.rescuehub.security.SecurityUtils;
import com.rescuehub.service.RiskScoreService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/risk-scores")
public class RiskScoreController {

    private final RiskScoreService riskScoreService;
    private final RoleGuard roleGuard;

    public RiskScoreController(RiskScoreService riskScoreService, RoleGuard roleGuard) {
        this.riskScoreService = riskScoreService;
        this.roleGuard = roleGuard;
    }

    /**
     * Admin-only telemetry view. Scores are ephemeral (reset on restart) and never
     * act as an authorization input — see RiskScoreService Javadoc.
     */
    @GetMapping
    public Map<String, Object> list() {
        User actor = SecurityUtils.currentUser();
        roleGuard.require(actor, Role.ADMIN);
        List<Map<String, Object>> scores = riskScoreService.getAllScores().entrySet().stream()
                .map(e -> ApiResponse.safeMap("workstationId", e.getKey(), "score", e.getValue()))
                .collect(Collectors.toList());
        return ApiResponse.data(scores);
    }
}
