package com.rescuehub.service;

import com.rescuehub.entity.RestoreTestLog;
import com.rescuehub.entity.User;
import com.rescuehub.enums.RestoreTestResult;
import com.rescuehub.enums.Role;
import com.rescuehub.repository.RestoreTestLogRepository;
import com.rescuehub.security.RoleGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class RestoreTestService {

    private final RestoreTestLogRepository repo;
    private final RoleGuard roleGuard;
    private final AuditService auditService;

    public RestoreTestService(RestoreTestLogRepository repo, RoleGuard roleGuard, AuditService auditService) {
        this.repo = repo;
        this.roleGuard = roleGuard;
        this.auditService = auditService;
    }

    @Transactional
    public RestoreTestLog record(User actor, Long backupRunId, RestoreTestResult result, String note,
                                  String ip, String workstationId) {
        roleGuard.require(actor, Role.ADMIN);
        RestoreTestLog log = new RestoreTestLog();
        log.setOrganizationId(actor.getOrganizationId());
        log.setPerformedByUserId(actor.getId());
        log.setBackupRunId(backupRunId);
        log.setResult(result);
        log.setNote(note);
        log.setPerformedAt(Instant.now());
        log = repo.save(log);

        auditService.log(actor.getId(), actor.getUsername(), "RESTORE_TEST_RECORDED",
                "RestoreTestLog", String.valueOf(log.getId()), actor.getOrganizationId(), ip, workstationId,
                null, "{\"result\":\"" + result + "\",\"backupRunId\":" + backupRunId + "}");
        return log;
    }

    @Transactional(readOnly = true)
    public Page<RestoreTestLog> list(User actor, Pageable pageable) {
        roleGuard.require(actor, Role.ADMIN);
        return repo.findByOrganizationId(actor.getOrganizationId(), pageable);
    }
}
