package com.rogeriofrsouza.bazaar.catalog.category;

import io.hypersistence.tsid.TSID;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record CategoryResponse(
        String id,
        String slug,
        String name,
        @Nullable List<CategoryResponse> children
) {
    public static CategoryResponse from(Category category, List<Category> children) {
        List<CategoryResponse> childrenList = children.stream()
                .map(child -> new CategoryResponse(TSID.from(child.getId()).toString(),
                        child.getSlug(),
                        child.getName(),
                        null))
                .toList();

        return new CategoryResponse(
                TSID.from(category.getId()).toString(),
                category.getSlug(),
                category.getName(),
                childrenList
        );
    }
}
