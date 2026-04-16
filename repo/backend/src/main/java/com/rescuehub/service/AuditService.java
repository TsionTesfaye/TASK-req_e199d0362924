package com.rescuehub.service;

import com.rescuehub.entity.AuditLog;
import com.rescuehub.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long actorId, String actorUsername, String actionCode, String objectType,
                    String objectId, Long orgId, String ip, String workstationId,
                    String before, String after) {
        AuditLog log = new AuditLog();
        log.setOrganizationId(orgId);
        log.setActorUserId(actorId);
        log.setActorUsernameSnapshot(actorUsername);
        log.setActionCode(actionCode);
        log.setObjectType(objectType);
        log.setObjectId(objectId);
        log.setIpAddress(ip);
        log.setWorkstationId(workstationId);
        log.setBeforeJson(before);
        log.setAfterJson(after);
        auditLogRepository.save(log);
    }
}
