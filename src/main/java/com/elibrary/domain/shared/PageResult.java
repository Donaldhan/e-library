package com.elibrary.domain.shared;

import java.util.List;
import java.util.function.Function;

/** Database-agnostic page result, so domain ports do not depend on Spring Data types. */
public record PageResult<T>(List<T> items, int page, int size, long totalElements) {

    public PageResult {
        items = List.copyOf(items);
    }

    public static <T> PageResult<T> of(List<T> items, PageQuery query, long totalElements) {
        return new PageResult<>(items, query.page(), query.size(), totalElements);
    }

    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        return new PageResult<>(items.stream().<R>map(mapper).toList(), page, size, totalElements);
    }

    public int totalPages() {
        return size == 0 ? 0 : (int) ((totalElements + size - 1) / size);
    }
}
