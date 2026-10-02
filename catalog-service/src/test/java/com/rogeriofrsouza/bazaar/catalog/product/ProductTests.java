package com.rogeriofrsouza.bazaar.catalog.product;

import io.hypersistence.tsid.TSID;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

class ProductTests {

    private static final Currency USD = Currency.getInstance("USD");

    @Test
    void createGeneratesTsidId() {
        Product product = Product.create("ABCD2345", "Phone X", null, new BigDecimal("499.90"), USD, null, 1L);

        assertThat(TSID.from(product.getId()).getInstant()).isCloseTo(Instant.now(), within(1, ChronoUnit.MINUTES));
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(product.getCreatedAt()).isNull();
    }

    @Test
    void rejectsBlankCode() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Product.create(" ", "Phone X", null, BigDecimal.ONE, USD, null, 1L))
                .withMessage("Product code must not be blank");
    }

    @Test
    void rejectsBlankName() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Product.create("ABCD2345", "", null, BigDecimal.ONE, USD, null, 1L))
                .withMessage("Product name must not be blank");
    }

    @Test
    void rejectsNegativePrice() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Product.create("ABCD2345", "Phone X", null, new BigDecimal("-0.01"), USD, null, 1L))
                .withMessage("Product price must not be negative");
    }
}
