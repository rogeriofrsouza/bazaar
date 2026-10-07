package com.rogeriofrsouza.bazaar.catalog.category;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String slug,
        String name,
        @Nullable List<CategoryResponse> children
) {
    public static CategoryResponse from(Category category, List<Category> children) {
        List<CategoryResponse> childrenList = children.stream()
                .map(child -> new CategoryResponse(child.getId(), child.getSlug(), child.getName(), null))
                .toList();

        return new CategoryResponse(
                category.getId(),
                category.getSlug(),
                category.getName(),
                childrenList
        );
    }
}
