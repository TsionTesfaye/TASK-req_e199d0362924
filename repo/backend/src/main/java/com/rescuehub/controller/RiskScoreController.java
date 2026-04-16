package com.rescuehub.controller;

import com.rescuehub.entity.User;
import com.rescuehub.enums.Role;
import com.rescuehub.security.RoleGuard;
import com.rescuehub.security.SecurityUtils;
import com.rescuehub.service.RiskScoreService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
        return ApiResponse.data(ApiResponse.safeMap(
                "ephemeral", riskScoreService.isEphemeral(),
                "scores", riskScoreService.getAllScores()));
    }
}
