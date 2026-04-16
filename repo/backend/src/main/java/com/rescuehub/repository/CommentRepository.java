package com.rescuehub.repository;

import com.rescuehub.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByOrganizationIdAndContentTypeAndContentId(Long orgId, String contentType, Long contentId, Pageable pageable);
    long countByContentTypeAndContentId(String contentType, Long contentId);
}
