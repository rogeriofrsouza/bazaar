package com.rogeriofrsouza.bazaar.catalog.category;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

class CategoryTests {

    @Test
    void createGeneratesUuidV7Id() {
        UUID parentId = UUID.randomUUID();
        Category category = Category.create("phones", "Phones", parentId);

        assertThat(category.getId().version()).isEqualTo(7);
        assertThat(Instant.ofEpochMilli(category.getId().getMostSignificantBits() >>> 16))
                .isCloseTo(Instant.now(), within(1, ChronoUnit.MINUTES));
        assertThat(category.getSlug()).isEqualTo("phones");
        assertThat(category.getName()).isEqualTo("Phones");
        assertThat(category.getParentId()).isEqualTo(parentId);
    }

    @Test
    void rejectsBlankSlug() {
        assertThatIllegalArgumentException().isThrownBy(() -> Category.create(" ", "Phones", null))
                .withMessage("Category slug must not be blank");
    }

    @Test
    void rejectsBlankName() {
        assertThatIllegalArgumentException().isThrownBy(() -> Category.create("phones", "", null))
                .withMessage("Category name must not be blank");
    }
}
