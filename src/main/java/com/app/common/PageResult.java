package com.app.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 分页结果封装
 */
@Data
@AllArgsConstructor
public class PageResult<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;

    public static <T> PageResult<T> of(Page<T> page) {
        return new PageResult<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }

    public static <T> PageResult<T> of(List<T> content, int page, int size, long total) {
        int totalPages = (int) Math.ceil((double) total / size);
        return new PageResult<>(
                content, page, size, total, totalPages,
                page < totalPages - 1
        );
    }

    public static <T> PageResult<T> empty() {
        return new PageResult<>(List.of(), 0, 10, 0, 0, false);
    }
}
