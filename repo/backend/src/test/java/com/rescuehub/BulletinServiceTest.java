package com.rescuehub;

import com.rescuehub.entity.Bulletin;
import com.rescuehub.entity.User;
import com.rescuehub.enums.BulletinStatus;
import com.rescuehub.enums.Role;
import com.rescuehub.exception.ForbiddenException;
import com.rescuehub.exception.NotFoundException;
import com.rescuehub.service.BulletinService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

class BulletinServiceTest extends BaseIntegrationTest {

    @Autowired
    private BulletinService bulletinService;

    private User moderatorUser() {
        return getOrCreateUser("test_moderator_bs", Role.MODERATOR);
    }

    @Test
    @Transactional
    void create_adminCreatesBulletinWithDraftStatus() {
        long nanos = System.nanoTime();
        Bulletin b = bulletinService.create(
                adminUser,
                "Test Bulletin " + nanos,
                "Body content for test bulletin " + nanos,
                "127.0.0.1", "ws");

        assertNotNull(b);
        assertNotNull(b.getId());
        assertEquals(BulletinStatus.DRAFT, b.getStatus());
        assertEquals("Test Bulletin " + nanos, b.getTitle());
        assertEquals(adminUser.getId(), b.getCreatedByUserId());
        assertEquals(adminUser.getOrganizationId(), b.getOrganizationId());
    }

    @Test
    @Transactional
    void create_clinicianNotAdminOrModeratorThrowsForbidden() {
        assertThrows(ForbiddenException.class, () ->
                bulletinService.create(
                        clinicianUser,
                        "Forbidden Bulletin",
                        "Body",
                        "127.0.0.1", "ws"));
    }

    @Test
    @Transactional
    void list_clinicianUserCanListBulletins() {
        long nanos = System.nanoTime();
        bulletinService.create(adminUser,
                "Listed Bulletin " + nanos, "Body " + nanos,
                "127.0.0.1", "ws");

        Page<Bulletin> page = bulletinService.list(clinicianUser, PageRequest.of(0, 20));

        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 1);
    }

    @Test
    @Transactional
    void updateStatus_adminModeratestoPublished() {
        long nanos = System.nanoTime();
        Bulletin b = bulletinService.create(
                adminUser,
                "To Publish " + nanos, "Body " + nanos,
                "127.0.0.1", "ws");
        assertEquals(BulletinStatus.DRAFT, b.getStatus());

        Bulletin published = bulletinService.updateStatus(
                adminUser, b.getId(), BulletinStatus.PUBLISHED,
                "127.0.0.1", "ws");

        assertEquals(BulletinStatus.PUBLISHED, published.getStatus());
        assertEquals(adminUser.getId(), published.getModeratedByUserId());
    }

    @Test
    @Transactional
    void getById_returnsCorrectBulletin() {
        long nanos = System.nanoTime();
        Bulletin created = bulletinService.create(
                adminUser,
                "Fetchable " + nanos, "Body " + nanos,
                "127.0.0.1", "ws");

        Bulletin fetched = bulletinService.getById(adminUser, created.getId());

        assertNotNull(fetched);
        assertEquals(created.getId(), fetched.getId());
        assertEquals(created.getTitle(), fetched.getTitle());
    }

    @Test
    @Transactional
    void getById_wrongIdThrowsNotFoundException() {
        assertThrows(NotFoundException.class,
                () -> bulletinService.getById(adminUser, Long.MAX_VALUE));
    }
}
