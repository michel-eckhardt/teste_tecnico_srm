package com.srm.creditengine.web.support;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Paginated response of the API contract: {@code { "content": [...], "page": {...} }}.
 *
 * @param content items of the requested page
 * @param page pagination metadata
 */
public record PageResponse<T>(List<T> content, PageMetadata page) {

    /**
     * @param number zero-based page index
     * @param size requested page size
     * @param totalElements total number of items matching the query
     * @param totalPages total number of pages
     */
    public record PageMetadata(int number, int size, long totalElements, int totalPages) {}

    public static <S, T> PageResponse<T> from(Page<S> page, Function<S, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                new PageMetadata(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()));
    }

    public static <T> PageResponse<T> of(List<T> content, int number, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) ((totalElements + size - 1) / size);
        return new PageResponse<>(content, new PageMetadata(number, size, totalElements, totalPages));
    }
}
