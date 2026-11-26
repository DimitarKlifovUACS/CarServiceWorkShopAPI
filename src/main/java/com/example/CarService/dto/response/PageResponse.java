package com.example.CarService.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * One page of a list plus the numbers a frontend needs for pagination.
 * (Spring's own Page object is not meant to be sent as JSON directly: its shape is not stable.)
 *
 * @param content       the items on this page
 * @param page          page number, starting at 0
 * @param size          requested page size
 * @param totalElements number of items across all pages
 * @param totalPages    number of pages
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
