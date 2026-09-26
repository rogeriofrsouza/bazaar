package com.rogeriofrsouza.bazaar.catalog.category;

import java.util.List;

public record CategoryResponse(
        String slug,
        String name,
        List<CategoryResponse> children
) {
    public static CategoryResponse from(Category category) {
        List<CategoryResponse> childrenList = category.getChildren()
                .stream()
                .map(child -> new CategoryResponse(child.getSlug(), child.getName(), null))
                .toList();

        return new CategoryResponse(
                category.getSlug(),
                category.getName(),
                childrenList
        );
    }
}
