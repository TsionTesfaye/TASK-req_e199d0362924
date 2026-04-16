package com.rescuehub;

import com.rescuehub.entity.Bulletin;
import com.rescuehub.entity.IncidentReport;
import com.rescuehub.service.BulletinService;
import com.rescuehub.service.IncidentService;
import com.rescuehub.service.SearchService;
import com.rescuehub.service.SearchService.SearchResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchServiceTest extends BaseIntegrationTest {

    @Autowired
    private SearchService searchService;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private BulletinService bulletinService;

    private IncidentReport createIncident(String suffix) {
        return incidentService.submit(
                frontDeskUser,
                "search-idem-" + suffix,
                "welfare",
                "Search test description " + suffix,
                "5th & Main",
                "Downtown",
                "5th & Main",
                null,
                false, false, false,
                "adult",
                "127.0.0.1", "ws");
    }

    private Bulletin createBulletin(String suffix) {
        return bulletinService.create(
                adminUser,
                "Search Test Bulletin " + suffix,
                "Bulletin body for search test " + suffix,
                "127.0.0.1", "ws");
    }

    @Test
    @Transactional
    void search_emptyQuery_returnsResults() {
        long nanos = System.nanoTime();
        createIncident(String.valueOf(nanos));
        createBulletin(String.valueOf(nanos));

        List<SearchResult> results = searchService.search(
                adminUser, "", null, null, 0, 50);

        assertNotNull(results);
        // After seeding, there must be at least 2 results (one incident + one bulletin)
        assertTrue(results.size() >= 2, "Empty query must return both incidents and bulletins");
    }

    @Test
    @Transactional
    void search_filteredByContentTypeIncident_onlyReturnsIncidents() {
        long nanos = System.nanoTime();
        createIncident("incident-only-" + nanos);
        createBulletin("incident-only-" + nanos);

        List<SearchResult> results = searchService.search(
                adminUser, "", "incident", null, 0, 50);

        assertNotNull(results);
        assertTrue(results.size() >= 1);
        results.forEach(r ->
                assertEquals("incident", r.contentType(),
                        "All results must be of contentType 'incident'"));
    }

    @Test
    @Transactional
    void search_filteredByContentTypeBulletin_onlyReturnsBulletins() {
        long nanos = System.nanoTime();
        createIncident("bulletin-only-" + nanos);
        createBulletin("bulletin-only-" + nanos);

        List<SearchResult> results = searchService.search(
                adminUser, "", "bulletin", null, 0, 50);

        assertNotNull(results);
        assertTrue(results.size() >= 1);
        results.forEach(r ->
                assertEquals("bulletin", r.contentType(),
                        "All results must be of contentType 'bulletin'"));
    }

    @Test
    @Transactional
    void search_sortRecent_doesNotThrow() {
        long nanos = System.nanoTime();
        createIncident("recent-sort-" + nanos);

        assertDoesNotThrow(() ->
                searchService.search(adminUser, "", null, "recent", 0, 20));
    }

    @Test
    @Transactional
    void search_sortPopular_doesNotThrow() {
        long nanos = System.nanoTime();
        createIncident("popular-sort-" + nanos);
        createBulletin("popular-sort-" + nanos);
        assertDoesNotThrow(() ->
                searchService.search(adminUser, "", null, "popular", 0, 20));
    }

    @Test
    @Transactional
    void search_sortFavorites_doesNotThrow() {
        long nanos = System.nanoTime();
        createIncident("fav-sort-" + nanos);
        createBulletin("fav-sort-" + nanos);
        assertDoesNotThrow(() ->
                searchService.search(adminUser, "", null, "favorites", 0, 20));
    }

    @Test
    @Transactional
    void search_sortComments_doesNotThrow() {
        long nanos = System.nanoTime();
        createIncident("comment-sort-" + nanos);
        createBulletin("comment-sort-" + nanos);
        assertDoesNotThrow(() ->
                searchService.search(adminUser, "", null, "comments", 0, 20));
    }

    @Test
    @Transactional
    void search_sortRecent_withBulletins_doesNotThrow() {
        long nanos = System.nanoTime();
        createIncident("recent-sort-b-" + nanos);
        createBulletin("recent-sort-b-" + nanos);
        assertDoesNotThrow(() ->
                searchService.search(adminUser, "", null, "recent", 0, 20));
    }

    @Test
    @Transactional
    void search_queryMatchesDescription_findsItem() {
        long nanos = System.nanoTime();
        String uniqueToken = "uniquetoken" + nanos;

        // Create incident with a unique token in the description
        incidentService.submit(
                frontDeskUser,
                "search-unique-" + nanos,
                "welfare",
                "Description containing " + uniqueToken,
                "5th & Main",
                "Downtown",
                "5th & Main",
                null,
                false, false, false,
                "adult",
                "127.0.0.1", "ws");

        List<SearchResult> results = searchService.search(
                adminUser, uniqueToken, "incident", null, 0, 50);

        assertNotNull(results);
        assertTrue(results.size() >= 1, "Query with unique token must find the matching incident");
        boolean found = results.stream()
                .filter(r -> "incident".equals(r.contentType()))
                .map(r -> (IncidentReport) r.item())
                .anyMatch(i -> i.getDescription().contains(uniqueToken));
        assertTrue(found, "The incident with the unique token must be in results");
    }
}
