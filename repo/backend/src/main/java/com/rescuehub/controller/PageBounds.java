package com.rescuehub.controller;

import com.rescuehub.exception.BusinessRuleException;
import org.springframework.data.domain.PageRequest;

/**
 * Pagination bounds guard. Central enforcement so every list endpoint
 * rejects negative pages and unbounded sizes consistently.
 */
public final class PageBounds {
    public static final int MAX_SIZE = 200;
    public static final int DEFAULT_SIZE = 20;

    private PageBounds() {}

    public static PageRequest of(int page, int size) {
        if (page < 0) throw new BusinessRuleException("page must be >= 0");
        if (size <= 0) throw new BusinessRuleException("size must be > 0");
        if (size > MAX_SIZE) throw new BusinessRuleException("size exceeds max " + MAX_SIZE);
        return PageRequest.of(page, size);
    }
}
