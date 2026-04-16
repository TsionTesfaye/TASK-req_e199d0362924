package com.rescuehub.controller;

import com.rescuehub.entity.IncidentMediaFile;
import com.rescuehub.entity.IncidentReport;
import com.rescuehub.entity.User;
import com.rescuehub.enums.IncidentStatus;
import com.rescuehub.repository.IncidentMediaFileRepository;
import com.rescuehub.security.SecurityUtils;
import com.rescuehub.service.IncidentService;
import com.rescuehub.service.MediaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;
    private final MediaService mediaService;
    private final IncidentMediaFileRepository mediaFileRepo;

    public IncidentController(IncidentService incidentService, MediaService mediaService,
                               IncidentMediaFileRepository mediaFileRepo) {
        this.incidentService = incidentService;
        this.mediaService = mediaService;
        this.mediaFileRepo = mediaFileRepo;
    }

    record SubmitRequest(
            @NotBlank String idempotencyKey, @NotBlank String category, @NotBlank String description,
            @NotBlank String approximateLocationText, String neighborhood, String nearestCrossStreets,
            String exactLocation, boolean isAnonymous, boolean involvesMinor, boolean isProtectedCase,
            String subjectAgeGroup) {}

    record ModerateRequest(@NotBlank String status) {}

    @PostMapping
    public Map<String, Object> submit(@Valid @RequestBody SubmitRequest req, HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        IncidentReport incident = incidentService.submit(actor, req.idempotencyKey(), req.category(),
                req.description(), req.approximateLocationText(), req.neighborhood(), req.nearestCrossStreets(),
                req.exactLocation(), req.isAnonymous(), req.involvesMinor(), req.isProtectedCase(),
                req.subjectAgeGroup(), request.getRemoteAddr(), SecurityUtils.currentWorkstationId());
        return ApiResponse.data(incident);
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        User actor = SecurityUtils.currentUser();
        Page<IncidentReport> incidents = incidentService.list(actor, PageBounds.of(page, size));
        return ApiResponse.list(incidents.getContent(), incidents.getTotalElements(), page, size);
    }

    record ReclassifyRequest(boolean isProtectedCase, boolean involvesMinor,
                              String exactLocation, String reason) {}

    @PostMapping("/{id}/reclassify")
    public Map<String, Object> reclassify(@PathVariable Long id, @RequestBody ReclassifyRequest req,
                                           HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(incidentService.reclassify(actor, id, req.isProtectedCase(),
                req.involvesMinor(), req.exactLocation(), req.reason(),
                request.getRemoteAddr(), SecurityUtils.currentWorkstationId()));
    }

    @GetMapping("/{id}/reveal-location")
    public Map<String, Object> revealLocation(@PathVariable Long id, HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        String exact = incidentService.revealExactLocation(actor, id,
                request.getRemoteAddr(), SecurityUtils.currentWorkstationId());
        return ApiResponse.data(ApiResponse.safeMap("exactLocation", exact));
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Long id) {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(incidentService.getById(actor, id));
    }

    @PostMapping("/{id}/moderate")
    public Map<String, Object> moderate(@PathVariable Long id, @Valid @RequestBody ModerateRequest req,
                                         HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        IncidentStatus status = IncidentStatus.valueOf(req.status().toUpperCase());
        IncidentReport incident = incidentService.moderate(actor, id, status,
                request.getRemoteAddr(), SecurityUtils.currentWorkstationId());
        return ApiResponse.data(incident);
    }

    @GetMapping("/{id}/media")
    public Map<String, Object> listMedia(@PathVariable Long id) {
        User actor = SecurityUtils.currentUser();
        // Validate incident belongs to actor's org before returning media
        incidentService.getById(actor, id);
        List<IncidentMediaFile> files = mediaFileRepo.findByIncidentReportId(id);
        return ApiResponse.data(files);
    }

    @PostMapping("/{id}/media")
    public Map<String, Object> uploadMedia(@PathVariable Long id,
                                            @RequestParam("file") MultipartFile file,
                                            HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        IncidentMediaFile media = mediaService.store(actor, id, file,
                request.getRemoteAddr(), SecurityUtils.currentWorkstationId());
        return ApiResponse.data(media);
    }
}
