package com.rescuehub;

import com.rescuehub.entity.ShelterResource;
import com.rescuehub.exception.ForbiddenException;
import com.rescuehub.exception.NotFoundException;
import com.rescuehub.service.ShelterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

class ShelterServiceTest extends BaseIntegrationTest {

    @Autowired
    private ShelterService shelterService;

    @Test
    @Transactional
    void create_adminCreatesShelterWithCorrectFields() {
        long nanos = System.nanoTime();
        String name = "Test Shelter " + nanos;

        ShelterResource sr = shelterService.create(
                adminUser, name, "FOOD", "Downtown",
                "123 Main St", null, null,
                "127.0.0.1", "ws");

        assertNotNull(sr);
        assertNotNull(sr.getId());
        assertEquals(name, sr.getName());
        assertEquals("FOOD", sr.getCategory());
        assertEquals("Downtown", sr.getNeighborhood());
        assertEquals("123 Main St", sr.getAddressText());
        assertTrue(sr.isActive());
        assertEquals(adminUser.getOrganizationId(), sr.getOrganizationId());
    }

    @Test
    @Transactional
    void create_nonAdminFrontDeskThrowsForbidden() {
        assertThrows(ForbiddenException.class, () ->
                shelterService.create(
                        frontDeskUser, "Forbidden Shelter", "SHELTER", "Eastside",
                        "99 East St", null, null,
                        "127.0.0.1", "ws"));
    }

    @Test
    @Transactional
    void list_frontDeskUserCanListShelters() {
        long nanos = System.nanoTime();
        shelterService.create(adminUser, "Listed Shelter " + nanos, "MEDICAL",
                "North", "1 North Ave", null, null, "127.0.0.1", "ws");

        Page<ShelterResource> page = shelterService.list(frontDeskUser, PageRequest.of(0, 20));

        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 1);
    }

    @Test
    @Transactional
    void getById_fetchesCorrectShelter() {
        long nanos = System.nanoTime();
        ShelterResource created = shelterService.create(
                adminUser, "Fetchable Shelter " + nanos, "FOOD", "West",
                "55 West Blvd", null, null, "127.0.0.1", "ws");

        ShelterResource fetched = shelterService.getById(adminUser, created.getId());

        assertNotNull(fetched);
        assertEquals(created.getId(), fetched.getId());
        assertEquals(created.getName(), fetched.getName());
    }

    @Test
    @Transactional
    void getById_wrongIdThrowsNotFoundException() {
        assertThrows(NotFoundException.class,
                () -> shelterService.getById(adminUser, Long.MAX_VALUE));
    }

    @Test
    @Transactional
    void update_adminCanUpdateShelterFields() {
        long nanos = System.nanoTime();
        ShelterResource original = shelterService.create(
                adminUser, "Original Name " + nanos, "FOOD", "South",
                "10 South St", null, null, "127.0.0.1", "ws");

        ShelterResource updated = shelterService.update(
                adminUser, original.getId(),
                "Updated Name " + nanos, "SHELTER", "North",
                "20 North Ave", null, null, true,
                "127.0.0.1", "ws");

        assertNotNull(updated);
        assertEquals("Updated Name " + nanos, updated.getName());
        assertEquals("SHELTER", updated.getCategory());
        assertEquals("North", updated.getNeighborhood());
        assertEquals("20 North Ave", updated.getAddressText());
    }
}
