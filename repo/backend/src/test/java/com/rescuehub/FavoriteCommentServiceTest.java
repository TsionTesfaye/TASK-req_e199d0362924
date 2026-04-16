package com.rescuehub;

import com.rescuehub.entity.*;
import com.rescuehub.exception.ConflictException;
import com.rescuehub.exception.NotFoundException;
import com.rescuehub.repository.*;
import com.rescuehub.service.BulletinService;
import com.rescuehub.service.FavoriteCommentService;
import com.rescuehub.service.IncidentService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class FavoriteCommentServiceTest extends BaseIntegrationTest {

    @Autowired private FavoriteCommentService favoriteCommentService;
    @Autowired private IncidentService incidentService;
    @Autowired private BulletinService bulletinService;
    @Autowired private FavoriteRepository favoriteRepo;
    @Autowired private CommentRepository commentRepo;
    @Autowired private IncidentReportRepository incidentRepo;
    @Autowired private BulletinRepository bulletinRepo;

    @PersistenceContext private EntityManager em;

    private IncidentReport createIncident() {
        return incidentService.submit(frontDeskUser, "idm-fav-" + System.nanoTime(),
                "welfare", "test description",
                "5th & Main", "Downtown", "5th & Main",
                null, false, false, false, "adult",
                "127.0.0.1", "ws");
    }

    @Test
    @Transactional
    void favoriteIncrement_incrementsFavoriteCount() {
        IncidentReport inc = createIncident();
        long before = inc.getFavoriteCount();

        favoriteCommentService.favorite(frontDeskUser, "incident", inc.getId());
        em.flush();
        em.clear();

        IncidentReport updated = incidentRepo.findById(inc.getId()).orElseThrow();
        assertEquals(before + 1, updated.getFavoriteCount(), "favorite count should increment by 1");
    }

    @Test
    @Transactional
    void favoriteIncident_duplicateThrowsConflict() {
        IncidentReport inc = createIncident();
        favoriteCommentService.favorite(frontDeskUser, "incident", inc.getId());

        assertThrows(ConflictException.class,
                () -> favoriteCommentService.favorite(frontDeskUser, "incident", inc.getId()),
                "second favorite on same content should throw ConflictException");
    }

    @Test
    @Transactional
    void unfavoriteIncident_decrementsFavoriteCount() {
        IncidentReport inc = createIncident();
        favoriteCommentService.favorite(frontDeskUser, "incident", inc.getId());
        em.flush();
        em.clear();

        IncidentReport afterFavorite = incidentRepo.findById(inc.getId()).orElseThrow();
        long countAfterFav = afterFavorite.getFavoriteCount();

        favoriteCommentService.unfavorite(frontDeskUser, "incident", inc.getId());
        em.flush();
        em.clear();

        IncidentReport afterUnfavorite = incidentRepo.findById(inc.getId()).orElseThrow();
        assertEquals(countAfterFav - 1, afterUnfavorite.getFavoriteCount(),
                "unfavorite should decrement count");
    }

    @Test
    @Transactional
    void commentOnIncident_incrementsCommentCount() {
        IncidentReport inc = createIncident();
        long before = inc.getCommentCount();

        Comment c = favoriteCommentService.comment(frontDeskUser, "incident", inc.getId(), "test comment");

        assertNotNull(c.getId());
        em.flush();
        em.clear();

        IncidentReport updated = incidentRepo.findById(inc.getId()).orElseThrow();
        assertEquals(before + 1, updated.getCommentCount(), "comment count should increment");
    }

    @Test
    @Transactional
    void listComments_returnsCommentsForContent() {
        IncidentReport inc = createIncident();
        favoriteCommentService.comment(frontDeskUser, "incident", inc.getId(), "comment one");
        favoriteCommentService.comment(adminUser, "incident", inc.getId(), "comment two");

        var page = favoriteCommentService.listComments(frontDeskUser, "incident",
                inc.getId(), PageRequest.of(0, 10));

        assertEquals(2, page.getTotalElements(), "should list all comments for the incident");
    }

    @Test
    @Transactional
    void favoriteOnCrossOrgContent_throwsNotFound() {
        // Create incident in testOrg, attempt favorite from a user in another org
        IncidentReport inc = createIncident();

        Organization otherOrg = new Organization();
        otherOrg.setCode("FAV-OTHER-" + System.nanoTime());
        otherOrg.setName("Other Org");
        otherOrg.setActive(true);
        otherOrg = orgRepo.save(otherOrg);

        User otherUser = new User();
        otherUser.setOrganizationId(otherOrg.getId());
        otherUser.setUsername("other_fav_" + System.nanoTime());
        otherUser.setPasswordHash("x");
        otherUser.setDisplayName("Other User");
        otherUser.setRole(com.rescuehub.enums.Role.FRONT_DESK);
        otherUser.setActive(true);
        otherUser.setFrozen(false);
        otherUser.setPasswordChangedAt(java.time.Instant.now());
        otherUser = userRepo.save(otherUser);

        final User finalOtherUser = otherUser;
        final Long incidentId = inc.getId();

        assertThrows(NotFoundException.class,
                () -> favoriteCommentService.favorite(finalOtherUser, "incident", incidentId),
                "favoriting cross-org content must throw NotFoundException");
    }

    @Test
    @Transactional
    void recordView_incrementsViewCount() {
        IncidentReport inc = createIncident();
        long before = inc.getViewCount();

        favoriteCommentService.recordView(frontDeskUser, "incident", inc.getId());
        em.flush();
        em.clear();

        IncidentReport updated = incidentRepo.findById(inc.getId()).orElseThrow();
        assertEquals(before + 1, updated.getViewCount(), "view count should increment by 1");
    }

    @Test
    @Transactional
    void listFavorites_returnsUserFavorites() {
        IncidentReport inc = createIncident();
        favoriteCommentService.favorite(frontDeskUser, "incident", inc.getId());

        var page = favoriteCommentService.listFavorites(frontDeskUser, PageRequest.of(0, 10));
        assertTrue(page.getTotalElements() >= 1, "listFavorites should include the favorited item");
        assertTrue(page.getContent().stream().anyMatch(f -> f.getContentId().equals(inc.getId())));
    }

    @Test
    @Transactional
    void favoriteBulletin_incrementsFavoriteCount() {
        Bulletin bulletin = bulletinService.create(adminUser,
                "Test Bulletin " + System.nanoTime(), "Body", "127.0.0.1", "ws");
        long before = bulletin.getFavoriteCount();

        favoriteCommentService.favorite(frontDeskUser, "bulletin", bulletin.getId());
        em.flush();
        em.clear();

        Bulletin updated = bulletinRepo.findById(bulletin.getId()).orElseThrow();
        assertEquals(before + 1, updated.getFavoriteCount(),
                "favoriting a bulletin should increment its favorite count");
    }

    @Test
    @Transactional
    void commentOnBulletin_incrementsCommentCount() {
        Bulletin bulletin = bulletinService.create(adminUser,
                "Comment Bulletin " + System.nanoTime(), "Body", "127.0.0.1", "ws");
        long before = bulletin.getCommentCount();

        favoriteCommentService.comment(frontDeskUser, "bulletin", bulletin.getId(), "nice bulletin");
        em.flush();
        em.clear();

        Bulletin updated = bulletinRepo.findById(bulletin.getId()).orElseThrow();
        assertEquals(before + 1, updated.getCommentCount(),
                "commenting on a bulletin should increment its comment count");
    }

    @Test
    @Transactional
    void recordView_bulletin_incrementsViewCount() {
        Bulletin bulletin = bulletinService.create(adminUser,
                "View Bulletin " + System.nanoTime(), "Body", "127.0.0.1", "ws");
        long before = bulletin.getViewCount();

        favoriteCommentService.recordView(frontDeskUser, "bulletin", bulletin.getId());
        em.flush();
        em.clear();

        Bulletin updated = bulletinRepo.findById(bulletin.getId()).orElseThrow();
        assertEquals(before + 1, updated.getViewCount(),
                "recording a view on a bulletin should increment its view count");
    }

    @Test
    @Transactional
    void unfavorite_neverFavorited_doesNotThrowAndDoesNotDecrement() {
        IncidentReport inc = createIncident();
        long before = inc.getFavoriteCount();

        // Unfavoriting something never favorited should be a no-op
        favoriteCommentService.unfavorite(frontDeskUser, "incident", inc.getId());
        em.flush();
        em.clear();

        IncidentReport updated = incidentRepo.findById(inc.getId()).orElseThrow();
        assertEquals(before, updated.getFavoriteCount(),
                "unfavoriting something never favorited should not change the count");
    }
}
