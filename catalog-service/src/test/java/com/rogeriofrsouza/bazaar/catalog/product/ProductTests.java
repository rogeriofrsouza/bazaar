package com.rogeriofrsouza.bazaar.catalog.product;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

class ProductTests {

    private static final Currency USD = Currency.getInstance("USD");

    private static final UUID CATEGORY_ID = UUID.randomUUID();

    @Test
    void createGeneratesUuidV7Id() {
        Product product = Product.create("Phone X", null, new BigDecimal("499.90"), USD, null, CATEGORY_ID);

        assertThat(product.getId().version()).isEqualTo(7);
        assertThat(Instant.ofEpochMilli(product.getId().getMostSignificantBits() >>> 16))
                .isCloseTo(Instant.now(), within(1, ChronoUnit.MINUTES));
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(product.getCreatedAt()).isNull();
    }

    @Test
    void rejectsBlankName() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Product.create("", null, BigDecimal.ONE, USD, null, CATEGORY_ID))
                .withMessage("Product name must not be blank");
    }

    @Test
    void rejectsNegativePrice() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Product.create("Phone X", null, new BigDecimal("-0.01"), USD, null, CATEGORY_ID))
                .withMessage("Product price must not be negative");
    }
}
