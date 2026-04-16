package com.rescuehub.controller;

import com.rescuehub.entity.User;
import com.rescuehub.security.SecurityUtils;
import com.rescuehub.service.ShelterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/shelters")
public class ShelterController {

    private final ShelterService shelterService;

    public ShelterController(ShelterService shelterService) {
        this.shelterService = shelterService;
    }

    record CreateRequest(@NotBlank String name, @NotBlank String category, String neighborhood,
                          @NotBlank String addressText, BigDecimal latitude, BigDecimal longitude) {}

    record UpdateRequest(@NotBlank String name, @NotBlank String category, String neighborhood,
                          @NotBlank String addressText, BigDecimal latitude, BigDecimal longitude, boolean isActive) {}

    @PostMapping
    public Map<String, Object> create(@Valid @RequestBody CreateRequest req, HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(shelterService.create(actor, req.name(), req.category(), req.neighborhood(),
                req.addressText(), req.latitude(), req.longitude(),
                request.getRemoteAddr(), SecurityUtils.currentWorkstationId()));
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        User actor = SecurityUtils.currentUser();
        var shelters = shelterService.list(actor, PageBounds.of(page, size));
        return ApiResponse.list(shelters.getContent(), shelters.getTotalElements(), page, size);
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Long id) {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(shelterService.getById(actor, id));
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @Valid @RequestBody UpdateRequest req,
                                       HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(shelterService.update(actor, id, req.name(), req.category(), req.neighborhood(),
                req.addressText(), req.latitude(), req.longitude(), req.isActive(),
                request.getRemoteAddr(), SecurityUtils.currentWorkstationId()));
    }
}
