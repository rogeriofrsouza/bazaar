package com.rogeriofrsouza.bazaar.catalog.category;

import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.util.Assert;

import java.util.UUID;

public class Category {

    @Id
    private final UUID id;

    private final String slug;

    private final String name;

    private final @Nullable UUID parentId;

    public Category(UUID id, String slug, String name, @Nullable UUID parentId) {
        Assert.hasText(slug, "Category slug must not be blank");
        Assert.hasText(name, "Category name must not be blank");
        this.id = id;
        this.slug = slug;
        this.name = name;
        this.parentId = parentId;
    }

    public static Category create(String slug, String name, @Nullable UUID parentId) {
        return new Category(UUID.ofEpochMillis(System.currentTimeMillis()), slug, name, parentId);
    }

    public UUID getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public @Nullable UUID getParentId() {
        return parentId;
    }

}
