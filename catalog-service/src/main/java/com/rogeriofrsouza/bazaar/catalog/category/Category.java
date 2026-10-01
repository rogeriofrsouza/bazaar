package com.rogeriofrsouza.bazaar.catalog.category;

import io.hypersistence.tsid.TSID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.util.Assert;

public class Category {

    @Id
    private final Long id;

    private final String slug;

    private final String name;

    private final @Nullable Long parentId;

    public Category(Long id, String slug, String name, @Nullable Long parentId) {
        Assert.hasText(slug, "Category slug must not be blank");
        Assert.hasText(name, "Category name must not be blank");
        this.id = id;
        this.slug = slug;
        this.name = name;
        this.parentId = parentId;
    }

    public static Category create(String slug, String name, @Nullable Long parentId) {
        return new Category(TSID.Factory.getTsid().toLong(), slug, name, parentId);
    }

    public Long getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public @Nullable Long getParentId() {
        return parentId;
    }

}
