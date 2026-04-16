package com.rescuehub.repository;

import com.rescuehub.entity.ViewEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ViewEventRepository extends JpaRepository<ViewEvent, Long> {
    long countByContentTypeAndContentId(String contentType, Long contentId);
}
