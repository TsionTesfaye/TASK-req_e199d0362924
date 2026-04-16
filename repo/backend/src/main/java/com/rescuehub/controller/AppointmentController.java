package com.rescuehub.controller;

import com.rescuehub.entity.Appointment;
import com.rescuehub.entity.User;
import com.rescuehub.enums.AppointmentStatus;
import com.rescuehub.security.SecurityUtils;
import com.rescuehub.service.AppointmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    record CreateRequest(@NotNull Long patientId,
                         @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduledDate,
                         @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime scheduledTime,
                         Long clinicianUserId) {}

    @PostMapping
    public Map<String, Object> create(@Valid @RequestBody CreateRequest req, HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        Appointment appt = appointmentService.create(actor, req.patientId(), req.scheduledDate(),
                req.scheduledTime(), req.clinicianUserId(), request.getRemoteAddr(), SecurityUtils.currentWorkstationId());
        return ApiResponse.data(appt);
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) String date,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        User actor = SecurityUtils.currentUser();
        LocalDate parsed = null;
        if (date != null && !date.isBlank()) {
            // Accept MM/DD/YYYY (business format) or ISO yyyy-MM-dd. Reject malformed strictly.
            try {
                if (date.matches("\\d{2}/\\d{2}/\\d{4}")) {
                    parsed = LocalDate.parse(date,
                            java.time.format.DateTimeFormatter.ofPattern("MM/dd/uuuu")
                                    .withResolverStyle(java.time.format.ResolverStyle.STRICT));
                } else {
                    parsed = LocalDate.parse(date);
                }
            } catch (Exception e) {
                throw new com.rescuehub.exception.BusinessRuleException(
                        "date must be MM/DD/YYYY or YYYY-MM-DD (strict): " + date);
            }
        }
        Page<Appointment> appts = appointmentService.list(actor, parsed, PageBounds.of(page, size));
        return ApiResponse.list(appts.getContent(), appts.getTotalElements(), page, size);
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Long id) {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(appointmentService.getById(actor, id));
    }

    @PutMapping("/{id}/status")
    public Map<String, Object> updateStatus(@PathVariable Long id, @RequestParam AppointmentStatus status,
                                             HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(appointmentService.updateStatus(actor, id, status,
                request.getRemoteAddr(), SecurityUtils.currentWorkstationId()));
    }
}
