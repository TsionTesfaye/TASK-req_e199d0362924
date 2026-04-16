package com.rescuehub;

import com.rescuehub.entity.IncidentReport;
import com.rescuehub.enums.IncidentStatus;
import com.rescuehub.exception.ForbiddenException;
import com.rescuehub.exception.NotFoundException;
import com.rescuehub.service.IncidentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

class IncidentServiceTest extends BaseIntegrationTest {

    @Autowired
    private IncidentService incidentService;

    private IncidentReport submitIncident(String suffix) {
        return incidentService.submit(
                frontDeskUser, "idm-inc-" + suffix,
                "welfare", "Description " + suffix,
                "4th & Oak", "Eastside", "4th & Oak",
                null, false, false, false, "adult",
                "127.0.0.1", "ws-test");
    }

    @Test
    @Transactional
    void getById_returnsIncidentForOrg() {
        IncidentReport inc = submitIncident(System.nanoTime() + "-get");
        IncidentReport fetched = incidentService.getById(adminUser, inc.getId());
        assertEquals(inc.getId(), fetched.getId());
        assertEquals("welfare", fetched.getCategory());
    }

    @Test
    @Transactional
    void getById_unknownId_throwsNotFoundException() {
        assertThrows(NotFoundException.class,
                () -> incidentService.getById(adminUser, Long.MAX_VALUE));
    }

    @Test
    @Transactional
    void list_returnsIncidentsForOrg() {
        submitIncident(System.nanoTime() + "-list");
        Page<IncidentReport> page = incidentService.list(adminUser, PageRequest.of(0, 20));
        assertNotNull(page);
        assertTrue(page.getTotalElements() >= 1);
    }

    @Test
    @Transactional
    void moderate_adminUpdatesStatus() {
        IncidentReport inc = submitIncident(System.nanoTime() + "-mod");
        assertEquals(IncidentStatus.SUBMITTED, inc.getStatus());

        IncidentReport moderated = incidentService.moderate(
                adminUser, inc.getId(), IncidentStatus.REVIEWED,
                "127.0.0.1", "ws-test");

        assertEquals(IncidentStatus.REVIEWED, moderated.getStatus());
    }

    @Test
    @Transactional
    void moderate_clinicianForbidden() {
        IncidentReport inc = submitIncident(System.nanoTime() + "-modfbd");
        assertThrows(ForbiddenException.class,
                () -> incidentService.moderate(
                        clinicianUser, inc.getId(), IncidentStatus.REVIEWED,
                        "127.0.0.1", "ws-test"));
    }

    @Test
    @Transactional
    void revealExactLocation_noEncryptedLocation_returnsNull() {
        // Submit without protected case — no ciphertext stored
        IncidentReport inc = submitIncident(System.nanoTime() + "-reveal");
        assertNull(inc.getExactLocationCiphertext());

        String result = incidentService.revealExactLocation(
                adminUser, inc.getId(), "127.0.0.1", "ws-test");
        assertNull(result);
    }

    @Test
    @Transactional
    void revealExactLocation_protectedCase_adminCanReveal() {
        // Submit as protected case with exact location — ciphertext stored
        IncidentReport inc = incidentService.submit(
                frontDeskUser, "idm-prot-rev-" + System.nanoTime(),
                "welfare", "Protected incident",
                "3rd & Elm", "Westside", "3rd & Elm",
                "999 Hidden Lane", false, false, true, "adult",
                "127.0.0.1", "ws-test");
        assertNotNull(inc.getExactLocationCiphertext());

        String revealed = incidentService.revealExactLocation(
                adminUser, inc.getId(), "127.0.0.1", "ws-test");
        assertEquals("999 Hidden Lane", revealed);
    }

    @Test
    @Transactional
    void revealExactLocation_frontDeskForbidden() {
        IncidentReport inc = incidentService.submit(
                frontDeskUser, "idm-reveal-fbd-" + System.nanoTime(),
                "welfare", "Protected",
                "2nd & Maple", "Northside", "2nd & Maple",
                "Sensitive Addr", false, true, false, "minor",
                "127.0.0.1", "ws-test");

        assertThrows(ForbiddenException.class,
                () -> incidentService.revealExactLocation(
                        frontDeskUser, inc.getId(), "127.0.0.1", "ws-test"));
    }
}
