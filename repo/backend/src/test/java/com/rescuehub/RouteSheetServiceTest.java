package com.rescuehub;

import com.rescuehub.entity.IncidentReport;
import com.rescuehub.entity.RouteSheet;
import com.rescuehub.entity.ShelterResource;
import com.rescuehub.exception.BusinessRuleException;
import com.rescuehub.exception.NotFoundException;
import com.rescuehub.repository.ShelterResourceRepository;
import com.rescuehub.service.IncidentService;
import com.rescuehub.service.RouteSheetService;
import com.rescuehub.service.ShelterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RouteSheetServiceTest extends BaseIntegrationTest {

    @Autowired
    private RouteSheetService routeSheetService;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private ShelterService shelterService;

    @Autowired
    private ShelterResourceRepository shelterResourceRepo;

    private IncidentReport submitIncident(String suffix) {
        return incidentService.submit(
                frontDeskUser,
                "route-idem-" + suffix,
                "welfare",
                "Route test description " + suffix,
                "5th & Main",
                "Downtown",
                "5th & Main",
                null,
                false, false, false,
                "adult",
                "127.0.0.1", "ws");
    }

    private ShelterResource createShelter(String suffix) {
        return shelterService.create(
                adminUser,
                "Route Shelter " + suffix,
                "FOOD",
                "Downtown",
                "100 Route Ave " + suffix,
                null, null,
                "127.0.0.1", "ws");
    }

    @Test
    @Transactional
    void generate_withExplicitShelter_returnsRouteSheetWithResourceIdAndTurnByTurn() {
        long nanos = System.nanoTime();
        IncidentReport incident = submitIncident(String.valueOf(nanos));
        ShelterResource shelter = createShelter(String.valueOf(nanos));

        RouteSheet sheet = routeSheetService.generate(
                adminUser, incident.getId(), shelter.getId(),
                "127.0.0.1", "ws");

        assertNotNull(sheet);
        assertNotNull(sheet.getId());
        assertEquals(shelter.getId(), sheet.getResourceId());
        assertNotNull(sheet.getRouteSummaryText());
        assertTrue(sheet.getRouteSummaryText().contains("Turn-by-turn:"),
                "Route summary must contain 'Turn-by-turn:'");
    }

    @Test
    @Transactional
    void generate_withAutoPick_resourceIdZero_autoSelectsShelter() {
        long nanos = System.nanoTime();
        IncidentReport incident = submitIncident("auto-" + nanos);
        // Ensure at least one shelter exists for auto-pick
        createShelter("auto-" + nanos);

        RouteSheet sheet = routeSheetService.generate(
                adminUser, incident.getId(), 0L,
                "127.0.0.1", "ws");

        assertNotNull(sheet);
        assertNotNull(sheet.getResourceId());
        assertTrue(sheet.getResourceId() > 0,
                "Auto-pick must select a valid shelter resource id");
    }

    @Test
    @Transactional
    void generate_noSheltersActive_resourceIdZero_throwsBusinessRuleException() {
        long nanos = System.nanoTime();
        IncidentReport incident = submitIncident("noshelter-" + nanos);

        // Deactivate all shelters for this org
        List<ShelterResource> active = shelterResourceRepo.findByOrganizationIdAndIsActive(
                testOrg.getId(), true);
        for (ShelterResource sr : active) {
            sr.setActive(false);
            shelterResourceRepo.save(sr);
        }

        assertThrows(BusinessRuleException.class, () ->
                routeSheetService.generate(
                        adminUser, incident.getId(), 0L,
                        "127.0.0.1", "ws"));
    }

    @Test
    @Transactional
    void generate_unknownIncidentId_throwsNotFoundException() {
        assertThrows(NotFoundException.class, () ->
                routeSheetService.generate(
                        adminUser, Long.MAX_VALUE, 0L,
                        "127.0.0.1", "ws"));
    }

    @Test
    @Transactional
    void generate_incidentWithGpsCoords_usesHaversineToSelectNearest() {
        long nanos = System.nanoTime();
        // Incident with lat,lon in nearestCrossStreets — triggers haversine path
        IncidentReport incident = incidentService.submit(
                frontDeskUser, "route-gps-" + nanos,
                "welfare", "GPS route test",
                "5th & Main", "Downtown",
                "37.77,-122.41",   // nearestCrossStreets as lat,lon
                null, false, false, false, "adult", "127.0.0.1", "ws");

        // Shelter very close to the incident's coordinates
        ShelterResource nearShelter = shelterService.create(
                adminUser, "Near Shelter " + nanos, "FOOD", "Downtown",
                "1 Near St " + nanos,
                new BigDecimal("37.77"), new BigDecimal("-122.41"),
                "127.0.0.1", "ws");

        // Shelter far away
        shelterService.create(
                adminUser, "Far Shelter " + nanos, "FOOD", "Uptown",
                "999 Far Ave " + nanos,
                new BigDecimal("40.00"), new BigDecimal("-120.00"),
                "127.0.0.1", "ws");

        RouteSheet sheet = routeSheetService.generate(
                adminUser, incident.getId(), 0L, "127.0.0.1", "ws");

        assertNotNull(sheet);
        assertEquals(nearShelter.getId(), sheet.getResourceId(),
                "Should select nearest shelter by haversine distance");
    }

    @Test
    @Transactional
    void generate_shelterWithoutCoords_routeSummaryContainsNoCoordsNote() {
        long nanos = System.nanoTime();
        IncidentReport incident = submitIncident("nocoord-" + nanos);

        // Shelter without GPS coordinates
        ShelterResource shelter = shelterService.create(
                adminUser, "No Coord Shelter " + nanos, "FOOD", "Eastside",
                "42 NoCoord Rd " + nanos,
                null, null,   // no lat/lon
                "127.0.0.1", "ws");

        RouteSheet sheet = routeSheetService.generate(
                adminUser, incident.getId(), shelter.getId(), "127.0.0.1", "ws");

        assertNotNull(sheet);
        assertTrue(sheet.getRouteSummaryText().contains("precise turn-by-turn unavailable"),
                "Route summary must note that precise GPS routing is unavailable");
    }

    @Test
    @Transactional
    void generate_neighborhoodMatch_selectsShelterByNeighborhood() {
        long nanos = System.nanoTime();
        // Incident in neighborhood "UniqueNeigh-<nanos>", no GPS coords in nearestCrossStreets
        IncidentReport incident = incidentService.submit(
                frontDeskUser, "route-neigh-" + nanos,
                "welfare", "Neighborhood route test",
                "5th & Main", "UniqueNeigh-" + nanos,
                "not-a-coordinate",   // invalid GPS — falls back to neighborhood match
                null, false, false, false, "adult", "127.0.0.1", "ws");

        ShelterResource shelter = shelterService.create(
                adminUser, "Neigh Shelter " + nanos, "FOOD", "UniqueNeigh-" + nanos,
                "10 Match St " + nanos, null, null, "127.0.0.1", "ws");

        RouteSheet sheet = routeSheetService.generate(
                adminUser, incident.getId(), 0L, "127.0.0.1", "ws");

        assertNotNull(sheet);
        assertEquals(shelter.getId(), sheet.getResourceId(),
                "Should select shelter matching incident's neighborhood");
    }
}
