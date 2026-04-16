package com.rescuehub.service;

import com.rescuehub.entity.Comment;
import com.rescuehub.entity.Favorite;
import com.rescuehub.entity.User;
import com.rescuehub.entity.ViewEvent;
import com.rescuehub.exception.ConflictException;
import com.rescuehub.repository.BulletinRepository;
import com.rescuehub.repository.CommentRepository;
import com.rescuehub.repository.FavoriteRepository;
import com.rescuehub.repository.IncidentReportRepository;
import com.rescuehub.repository.ViewEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class FavoriteCommentService {

    private final FavoriteRepository favoriteRepo;
    private final CommentRepository commentRepo;
    private final ViewEventRepository viewEventRepo;
    private final IncidentReportRepository incidentRepo;
    private final BulletinRepository bulletinRepo;
    private final AuditService auditService;
    private final RankingService rankingService;

    public FavoriteCommentService(FavoriteRepository favoriteRepo, CommentRepository commentRepo,
                                   ViewEventRepository viewEventRepo,
                                   IncidentReportRepository incidentRepo,
                                   BulletinRepository bulletinRepo,
                                   AuditService auditService,
                                   RankingService rankingService) {
        this.favoriteRepo = favoriteRepo;
        this.commentRepo = commentRepo;
        this.viewEventRepo = viewEventRepo;
        this.incidentRepo = incidentRepo;
        this.bulletinRepo = bulletinRepo;
        this.auditService = auditService;
        this.rankingService = rankingService;
    }

    /** Verify the content entity belongs to orgId, then increment/decrement its counter. */
    private void adjustFavoriteCount(Long orgId, String contentType, Long contentId, int delta) {
        if ("incident".equalsIgnoreCase(contentType)) {
            incidentRepo.findByOrganizationIdAndId(orgId, contentId)
                    .orElseThrow(() -> new com.rescuehub.exception.NotFoundException("Incident not found"));
            incidentRepo.adjustFavoriteCount(orgId, contentId, delta);
        } else if ("bulletin".equalsIgnoreCase(contentType)) {
            bulletinRepo.findByOrganizationIdAndId(orgId, contentId)
                    .orElseThrow(() -> new com.rescuehub.exception.NotFoundException("Bulletin not found"));
            bulletinRepo.adjustFavoriteCount(orgId, contentId, delta);
        }
    }

    private void incrementCommentCount(Long orgId, String contentType, Long contentId) {
        if ("incident".equalsIgnoreCase(contentType)) {
            incidentRepo.findByOrganizationIdAndId(orgId, contentId)
                    .orElseThrow(() -> new com.rescuehub.exception.NotFoundException("Incident not found"));
            incidentRepo.incrementCommentCount(orgId, contentId);
        } else if ("bulletin".equalsIgnoreCase(contentType)) {
            bulletinRepo.findByOrganizationIdAndId(orgId, contentId)
                    .orElseThrow(() -> new com.rescuehub.exception.NotFoundException("Bulletin not found"));
            bulletinRepo.incrementCommentCount(orgId, contentId);
        }
    }

    private void incrementViewCount(Long orgId, String contentType, Long contentId) {
        if ("incident".equalsIgnoreCase(contentType)) {
            incidentRepo.findByOrganizationIdAndId(orgId, contentId)
                    .orElseThrow(() -> new com.rescuehub.exception.NotFoundException("Incident not found"));
            incidentRepo.incrementViewCount(orgId, contentId);
        } else if ("bulletin".equalsIgnoreCase(contentType)) {
            bulletinRepo.findByOrganizationIdAndId(orgId, contentId)
                    .orElseThrow(() -> new com.rescuehub.exception.NotFoundException("Bulletin not found"));
            bulletinRepo.incrementViewCount(orgId, contentId);
        }
    }

    @Transactional
    public Favorite favorite(User actor, String contentType, Long contentId) {
        if (favoriteRepo.existsByOrganizationIdAndUserIdAndContentTypeAndContentId(
                actor.getOrganizationId(), actor.getId(), contentType, contentId)) {
            throw new ConflictException("Already favorited");
        }
        Favorite f = new Favorite();
        f.setOrganizationId(actor.getOrganizationId());
        f.setUserId(actor.getId());
        f.setContentType(contentType);
        f.setContentId(contentId);
        f = favoriteRepo.save(f);
        adjustFavoriteCount(actor.getOrganizationId(), contentType, contentId, +1);
        rankingService.syncScore(actor.getOrganizationId(), contentType, contentId);
        return f;
    }

    @Transactional
    public void unfavorite(User actor, String contentType, Long contentId) {
        boolean existed = favoriteRepo.findByOrganizationIdAndUserIdAndContentTypeAndContentId(
                actor.getOrganizationId(), actor.getId(), contentType, contentId)
                .map(fav -> { favoriteRepo.delete(fav); return true; })
                .orElse(false);
        if (existed) {
            adjustFavoriteCount(actor.getOrganizationId(), contentType, contentId, -1);
        }
        rankingService.syncScore(actor.getOrganizationId(), contentType, contentId);
    }

    @Transactional
    public Comment comment(User actor, String contentType, Long contentId, String body) {
        Comment c = new Comment();
        c.setOrganizationId(actor.getOrganizationId());
        c.setUserId(actor.getId());
        c.setContentType(contentType);
        c.setContentId(contentId);
        c.setBody(body);
        c = commentRepo.save(c);
        auditService.log(actor.getId(), actor.getUsername(), "COMMENT_CREATE",
                "Comment", String.valueOf(c.getId()), actor.getOrganizationId(), null, null, null, null);
        incrementCommentCount(actor.getOrganizationId(), contentType, contentId);
        rankingService.syncScore(actor.getOrganizationId(), contentType, contentId);
        return c;
    }

    @Transactional(readOnly = true)
    public Page<Comment> listComments(User actor, String contentType, Long contentId, Pageable pageable) {
        return commentRepo.findByOrganizationIdAndContentTypeAndContentId(
                actor.getOrganizationId(), contentType, contentId, pageable);
    }

    @Transactional
    public void recordView(User actor, String contentType, Long contentId) {
        ViewEvent ve = new ViewEvent();
        ve.setOrganizationId(actor.getOrganizationId());
        ve.setUserId(actor.getId());
        ve.setContentType(contentType);
        ve.setContentId(contentId);
        ve.setViewedAt(Instant.now());
        viewEventRepo.save(ve);
        incrementViewCount(actor.getOrganizationId(), contentType, contentId);
        rankingService.syncScore(actor.getOrganizationId(), contentType, contentId);
    }

    @Transactional(readOnly = true)
    public Page<Favorite> listFavorites(User actor, Pageable pageable) {
        return favoriteRepo.findByOrganizationIdAndUserId(actor.getOrganizationId(), actor.getId(), pageable);
    }
}
