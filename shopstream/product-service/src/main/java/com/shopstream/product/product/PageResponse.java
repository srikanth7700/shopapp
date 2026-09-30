package com.shopstream.product.product;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * A stable JSON shape for paged results. Returning Spring's Page directly
 * leaks internal fields and Spring warns against it.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
