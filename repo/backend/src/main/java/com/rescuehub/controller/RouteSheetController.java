package com.rescuehub.controller;

import com.rescuehub.entity.User;
import com.rescuehub.security.SecurityUtils;
import com.rescuehub.service.RouteSheetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/routesheets")
public class RouteSheetController {

    private final RouteSheetService routeSheetService;

    public RouteSheetController(RouteSheetService routeSheetService) {
        this.routeSheetService = routeSheetService;
    }

    record GenerateRequest(@NotNull Long incidentId, @NotNull Long resourceId) {}

    @PostMapping
    public Map<String, Object> generate(@Valid @RequestBody GenerateRequest req, HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(routeSheetService.generate(actor, req.incidentId(), req.resourceId(),
                request.getRemoteAddr(), SecurityUtils.currentWorkstationId()));
    }
}
