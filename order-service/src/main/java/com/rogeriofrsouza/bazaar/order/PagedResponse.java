package com.rogeriofrsouza.bazaar.order;

import java.util.List;

public record PagedResponse<T>(
        List<T> content,
        PageMetadata page
) {
    public PagedResponse {
        content = content == null ? List.of() : content;
    }

    public record PageMetadata(
            long size,
            long number,
            long totalElements,
            long totalPages
    ) {
    }
}
