package com.rescuehub.service;

import com.rescuehub.entity.IncidentMediaFile;
import com.rescuehub.entity.IncidentReport;
import com.rescuehub.entity.User;
import com.rescuehub.exception.BusinessRuleException;
import com.rescuehub.exception.ForbiddenException;
import com.rescuehub.exception.NotFoundException;
import com.rescuehub.repository.IncidentMediaFileRepository;
import com.rescuehub.repository.IncidentReportRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Set;

@Service
public class MediaService {

    private static final long MAX_SIZE = 50 * 1024 * 1024L; // 50MB
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "video/mp4", "video/quicktime");

    @Value("${rescuehub.storage.dir}")
    private String storageDir;

    private final IncidentMediaFileRepository mediaRepo;
    private final IncidentReportRepository incidentRepo;
    private final AuditService auditService;

    public MediaService(IncidentMediaFileRepository mediaRepo,
                        IncidentReportRepository incidentRepo,
                        AuditService auditService) {
        this.mediaRepo = mediaRepo;
        this.incidentRepo = incidentRepo;
        this.auditService = auditService;
    }

    @Transactional
    public IncidentMediaFile store(User actor, Long incidentId, MultipartFile file,
                                    String ip, String workstationId) {
        if (actor == null) throw new ForbiddenException("Authentication required");
        // Object-level authorization: incident must exist AND belong to the actor's organization
        IncidentReport incident = incidentRepo
                .findByOrganizationIdAndId(actor.getOrganizationId(), incidentId)
                .orElseThrow(() -> new NotFoundException("Incident not found: " + incidentId));
        // Protected-case incidents may only receive media from privileged roles
        if ((incident.isProtectedCase() || incident.isInvolvesMinor())
                && actor.getRole() != com.rescuehub.enums.Role.ADMIN
                && actor.getRole() != com.rescuehub.enums.Role.MODERATOR
                && actor.getRole() != com.rescuehub.enums.Role.QUALITY) {
            throw new ForbiddenException("Role not permitted to upload media to a protected-case incident");
        }
        if (file.isEmpty()) throw new BusinessRuleException("File is empty");
        if (file.getSize() > MAX_SIZE) throw new BusinessRuleException("File exceeds 50MB limit");

        String contentType = file.getContentType();
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessRuleException("File type not allowed: " + contentType);
        }

        try {
            byte[] bytes = file.getBytes();
            String sha256 = computeSha256(bytes);

            Path dir = Paths.get(storageDir, "media", String.valueOf(incidentId));
            Files.createDirectories(dir);
            String fileName = sha256 + "-" + sanitize(file.getOriginalFilename());
            Path dest = dir.resolve(fileName);
            Files.write(dest, bytes);

            IncidentMediaFile media = new IncidentMediaFile();
            media.setIncidentReportId(incidentId);
            media.setFileName(file.getOriginalFilename());
            media.setFileType(contentType);
            media.setFileSizeBytes(file.getSize());
            media.setSha256Hash(sha256);
            media.setStoragePath(dest.toString());
            media.setUploadedByUserId(actor.getId());
            IncidentMediaFile saved = mediaRepo.save(media);
            auditService.log(actor.getId(), actor.getUsername(), "INCIDENT_MEDIA_UPLOAD",
                    "IncidentMediaFile", String.valueOf(saved.getId()),
                    actor.getOrganizationId(), ip, workstationId, null,
                    "{\"incidentId\":" + incidentId + ",\"size\":" + file.getSize()
                            + ",\"sha256\":\"" + sha256 + "\"}");
            return saved;
        } catch (IOException e) {
            throw new BusinessRuleException("File storage failed: " + e.getMessage());
        }
    }

    private String computeSha256(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 failed", e);
        }
    }

    private String sanitize(String name) {
        if (name == null) return "upload";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
