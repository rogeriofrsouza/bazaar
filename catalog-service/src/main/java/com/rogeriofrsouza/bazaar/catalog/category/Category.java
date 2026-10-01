package com.rogeriofrsouza.bazaar.catalog.category;

import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;

public class Category {

    @Id
    private Long id;

    private String slug;

    private String name;

    private @Nullable Long parentId;

    protected Category() {
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
