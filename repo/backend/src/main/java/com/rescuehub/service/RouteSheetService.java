package com.rescuehub.service;

import com.rescuehub.entity.IncidentReport;
import com.rescuehub.entity.RouteSheet;
import com.rescuehub.entity.ShelterResource;
import com.rescuehub.entity.User;
import com.rescuehub.enums.Role;
import com.rescuehub.exception.BusinessRuleException;
import com.rescuehub.exception.NotFoundException;
import com.rescuehub.repository.IncidentReportRepository;
import com.rescuehub.repository.RouteSheetRepository;
import com.rescuehub.repository.ShelterResourceRepository;
import com.rescuehub.security.RoleGuard;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;

@Service
public class RouteSheetService {

    @Value("${rescuehub.storage.dir}")
    private String storageDir;

    private final IncidentReportRepository incidentRepo;
    private final ShelterResourceRepository shelterRepo;
    private final RouteSheetRepository routeSheetRepo;
    private final AuditService auditService;
    private final RoleGuard roleGuard;

    public RouteSheetService(IncidentReportRepository incidentRepo, ShelterResourceRepository shelterRepo,
                              RouteSheetRepository routeSheetRepo, AuditService auditService,
                              RoleGuard roleGuard) {
        this.incidentRepo = incidentRepo;
        this.shelterRepo = shelterRepo;
        this.routeSheetRepo = routeSheetRepo;
        this.auditService = auditService;
        this.roleGuard = roleGuard;
    }

    @Transactional
    public RouteSheet generate(User actor, Long incidentId, Long resourceId, String ip, String workstationId) {
        if (actor == null) throw new com.rescuehub.exception.ForbiddenException("Authentication required");
        IncidentReport incident = incidentRepo.findByOrganizationIdAndId(actor.getOrganizationId(), incidentId)
                .orElseThrow(() -> new NotFoundException("Incident not found"));

        // Check privacy for exact location
        boolean canRevealExact = roleGuard.hasRole(actor, Role.ADMIN, Role.MODERATOR, Role.QUALITY);
        String originDescription;
        if ((incident.isProtectedCase() || incident.isInvolvesMinor()) && !canRevealExact) {
            originDescription = incident.getNeighborhood() != null
                    ? "Neighborhood: " + incident.getNeighborhood()
                    : incident.getApproximateLocationText();
        } else {
            originDescription = incident.getApproximateLocationText();
        }

        // Resolve nearest shelter. If caller specified one (>0) we use it as a hint;
        // otherwise (and always when caller passes 0), pick the closest active shelter
        // to the incident neighborhood / parsed coordinates.
        ShelterResource nearest;
        if (resourceId != null && resourceId > 0) {
            nearest = shelterRepo.findByOrganizationIdAndId(actor.getOrganizationId(), resourceId)
                    .orElseThrow(() -> new NotFoundException("Shelter/resource not found"));
        } else {
            nearest = findNearest(actor.getOrganizationId(), incident);
            if (nearest == null) {
                throw new BusinessRuleException("No active shelters/resources to route to");
            }
        }

        java.util.List<String> turns = buildTurnByTurn(originDescription, nearest);
        String summary = buildRouteSummary(incident, nearest, originDescription, turns);
        String filePath = writeRouteFile(incident.getId(), summary);

        RouteSheet sheet = new RouteSheet();
        sheet.setIncidentReportId(incidentId);
        sheet.setResourceId(nearest.getId());
        sheet.setGeneratedByUserId(actor.getId());
        sheet.setRouteSummaryText(summary);
        sheet.setPrintableFilePath(filePath);
        sheet = routeSheetRepo.save(sheet);

        auditService.log(actor.getId(), actor.getUsername(), "ROUTE_SHEET_GENERATED",
                "RouteSheet", String.valueOf(sheet.getId()), actor.getOrganizationId(), ip, workstationId,
                null, "{\"incidentId\":" + incidentId + ",\"resourceId\":" + nearest.getId() + "}");

        return sheet;
    }

    /**
     * Pick the nearest active shelter:
     *  1. If incident.nearestCrossStreets contains "lat,lon", use haversine across all shelters with coords.
     *  2. Otherwise prefer shelters in the same neighborhood as the incident.
     *  3. Otherwise any shelter with coords (closest to the org centroid — first by id).
     *  4. Otherwise the first active shelter.
     */
    private ShelterResource findNearest(Long orgId, IncidentReport incident) {
        List<ShelterResource> active = shelterRepo.findByOrganizationIdAndIsActive(orgId, true);
        if (active.isEmpty()) return null;

        double[] origin = parseLatLon(incident.getNearestCrossStreets());
        if (origin != null) {
            ShelterResource best = null;
            double bestKm = Double.MAX_VALUE;
            for (ShelterResource s : active) {
                if (s.getLatitude() == null || s.getLongitude() == null) continue;
                double km = haversine(origin[0], origin[1],
                        s.getLatitude().doubleValue(), s.getLongitude().doubleValue());
                if (km < bestKm) { bestKm = km; best = s; }
            }
            if (best != null) return best;
        }

        if (incident.getNeighborhood() != null) {
            for (ShelterResource s : active) {
                if (incident.getNeighborhood().equalsIgnoreCase(s.getNeighborhood())) return s;
            }
        }
        return active.get(0);
    }

    private double[] parseLatLon(String txt) {
        if (txt == null) return null;
        String t = txt.trim();
        if (!t.matches("-?\\d+(\\.\\d+)?\\s*,\\s*-?\\d+(\\.\\d+)?")) return null;
        String[] parts = t.split(",");
        try {
            return new double[]{Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim())};
        } catch (Exception e) { return null; }
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * Generate basic turn-by-turn text. Uses haversine bearing if both endpoints have coords,
     * otherwise falls back to a structured directions summary (per design.md §10.3).
     */
    private List<String> buildTurnByTurn(String originText, ShelterResource resource) {
        java.util.List<String> steps = new java.util.ArrayList<>();
        steps.add("1. Start at: " + originText);
        if (resource.getLatitude() != null && resource.getLongitude() != null) {
            steps.add("2. Head toward " + cardinal(resource.getLatitude().doubleValue(),
                    resource.getLongitude().doubleValue()) + " on the nearest main road.");
            steps.add("3. Continue along arterial roads following posted signage for "
                    + (resource.getNeighborhood() != null ? resource.getNeighborhood() : "the destination") + ".");
            steps.add("4. Arrive at: " + resource.getName() + " (" + resource.getAddressText() + ").");
        } else {
            steps.add("2. Use posted street signs to navigate toward "
                    + (resource.getNeighborhood() != null ? resource.getNeighborhood() : "the destination") + ".");
            steps.add("3. Arrive at: " + resource.getName() + " (" + resource.getAddressText() + ").");
            steps.add("Note: precise turn-by-turn unavailable — exact GPS routing not configured.");
        }
        return steps;
    }

    private String cardinal(double lat, double lon) {
        // crude bearing from approximate org centroid (37.77, -122.42 — sample shelter SF area)
        double dLat = lat - 37.77;
        double dLon = lon - (-122.42);
        if (Math.abs(dLat) >= Math.abs(dLon)) return dLat >= 0 ? "north" : "south";
        return dLon >= 0 ? "east" : "west";
    }

    private String buildRouteSummary(IncidentReport incident, ShelterResource resource,
                                      String origin, List<String> turns) {
        StringBuilder sb = new StringBuilder();
        sb.append("ROUTE SHEET\n");
        sb.append("Generated: ").append(Instant.now()).append("\n");
        sb.append("Incident ID: ").append(incident.getId()).append("\n");
        sb.append("Category: ").append(incident.getCategory()).append("\n");
        sb.append("From (approximate): ").append(origin).append("\n");
        sb.append("To: ").append(resource.getName()).append("\n");
        sb.append("Address: ").append(resource.getAddressText()).append("\n");
        if (resource.getNeighborhood() != null) sb.append("Neighborhood: ").append(resource.getNeighborhood()).append("\n");
        sb.append("\nTurn-by-turn:\n");
        for (String t : turns) sb.append("  ").append(t).append("\n");
        if (incident.isProtectedCase() || incident.isInvolvesMinor()) {
            sb.append("\nPRIVACY: protected/minor case — exact origin intentionally hidden.\n");
        }
        return sb.toString();
    }

    private String writeRouteFile(Long incidentId, String content) {
        try {
            Path dir = Paths.get(storageDir, "routesheets");
            Files.createDirectories(dir);
            Path file = dir.resolve("route-incident-" + incidentId + "-" + Instant.now().toEpochMilli() + ".txt");
            Files.writeString(file, content);
            return file.toString();
        } catch (IOException e) {
            return null; // file write failure is non-fatal for the record
        }
    }
}
