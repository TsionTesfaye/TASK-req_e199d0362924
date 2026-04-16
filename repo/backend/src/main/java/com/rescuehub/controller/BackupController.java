package com.rescuehub.controller;

import com.rescuehub.entity.User;
import com.rescuehub.enums.RestoreTestResult;
import com.rescuehub.security.SecurityUtils;
import com.rescuehub.service.BackupService;
import com.rescuehub.service.RestoreTestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/backups")
public class BackupController {

    private final BackupService backupService;
    private final RestoreTestService restoreTestService;

    public BackupController(BackupService backupService, RestoreTestService restoreTestService) {
        this.backupService = backupService;
        this.restoreTestService = restoreTestService;
    }

    record RestoreTestRequest(@NotNull RestoreTestResult result, String note) {}

    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        User actor = SecurityUtils.currentUser();
        var runs = backupService.list(actor, PageBounds.of(page, size));
        return ApiResponse.list(runs.getContent(), runs.getTotalElements(), page, size);
    }

    @PostMapping("/run")
    public Map<String, Object> runBackup() {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(backupService.runBackupAsActor(actor));
    }

    @PostMapping("/{id}/restore-test")
    public Map<String, Object> recordRestoreTest(@PathVariable Long id,
                                                   @Valid @RequestBody RestoreTestRequest req,
                                                   HttpServletRequest request) {
        User actor = SecurityUtils.currentUser();
        return ApiResponse.data(restoreTestService.record(actor, id, req.result(), req.note(),
                request.getRemoteAddr(), SecurityUtils.currentWorkstationId()));
    }
}
