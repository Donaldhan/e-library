package com.elibrary.interfaces.http;

import com.elibrary.domain.shared.PageResult;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <T> PageResponse<T> from(PageResult<T> result) {
        return new PageResponse<>(result.items(), result.page(), result.size(),
                result.totalElements(), result.totalPages());
    }
}
