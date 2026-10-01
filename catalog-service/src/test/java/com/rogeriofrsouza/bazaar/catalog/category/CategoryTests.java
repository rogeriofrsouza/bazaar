package com.rogeriofrsouza.bazaar.catalog.category;

import io.hypersistence.tsid.TSID;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

class CategoryTests {

    @Test
    void createGeneratesTsidId() {
        Category category = Category.create("phones", "Phones", 1L);

        assertThat(TSID.from(category.getId()).getInstant()).isCloseTo(Instant.now(), within(1, ChronoUnit.MINUTES));
        assertThat(category.getSlug()).isEqualTo("phones");
        assertThat(category.getName()).isEqualTo("Phones");
        assertThat(category.getParentId()).isEqualTo(1L);
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
