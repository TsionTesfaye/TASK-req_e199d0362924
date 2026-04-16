package com.rescuehub;

import com.rescuehub.entity.IncidentMediaFile;
import com.rescuehub.entity.IncidentReport;
import com.rescuehub.exception.BusinessRuleException;
import com.rescuehub.exception.ForbiddenException;
import com.rescuehub.service.IncidentService;
import com.rescuehub.service.MediaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

class MediaServiceTest extends BaseIntegrationTest {

    @Autowired
    private MediaService mediaService;

    @Autowired
    private IncidentService incidentService;

    private IncidentReport createIncident(boolean protectedCase) {
        long nanos = System.nanoTime();
        return incidentService.submit(
                frontDeskUser,
                "media-idem-" + nanos,
                "welfare",
                "Media test incident " + nanos,
                "5th & Main",
                "Downtown",
                "5th & Main",
                protectedCase ? "999 Protected Ln" : null,
                false,
                false,
                protectedCase,
                "adult",
                "127.0.0.1", "ws");
    }

    @Test
    @Transactional
    void store_validImageUpload_succeeds() {
        IncidentReport incident = createIncident(false);

        byte[] imageBytes = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46
        };
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                imageBytes);

        IncidentMediaFile saved = mediaService.store(
                adminUser, incident.getId(), file,
                "127.0.0.1", "ws");

        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals("test-image.jpg", saved.getFileName());
        assertEquals("image/jpeg", saved.getFileType());
        assertEquals(adminUser.getId(), saved.getUploadedByUserId());
        assertNotNull(saved.getSha256Hash());
        assertNotNull(saved.getStoragePath());
    }

    @Test
    @Transactional
    void store_emptyFile_throwsBusinessRuleException() {
        IncidentReport incident = createIncident(false);

        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]);

        assertThrows(BusinessRuleException.class, () ->
                mediaService.store(
                        adminUser, incident.getId(), emptyFile,
                        "127.0.0.1", "ws"));
    }

    @Test
    @Transactional
    void store_wrongContentType_throwsBusinessRuleException() {
        IncidentReport incident = createIncident(false);

        byte[] pdfBytes = new byte[]{0x25, 0x50, 0x44, 0x46};
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                pdfBytes);

        assertThrows(BusinessRuleException.class, () ->
                mediaService.store(
                        adminUser, incident.getId(), pdfFile,
                        "127.0.0.1", "ws"));
    }

    @Test
    @Transactional
    void store_protectedIncidentUnprivilegedRole_throwsForbiddenException() {
        IncidentReport protectedIncident = createIncident(true);

        byte[] imageBytes = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46
        };
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sensitive.jpg",
                "image/jpeg",
                imageBytes);

        // frontDeskUser does not have ADMIN/MODERATOR/QUALITY role
        assertThrows(ForbiddenException.class, () ->
                mediaService.store(
                        frontDeskUser, protectedIncident.getId(), file,
                        "127.0.0.1", "ws"));
    }
}
