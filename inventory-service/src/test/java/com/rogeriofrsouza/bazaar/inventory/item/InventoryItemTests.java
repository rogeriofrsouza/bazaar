package com.rogeriofrsouza.bazaar.inventory.item;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

class InventoryItemTests {

    private static final UUID PRODUCT_ID = UUID.randomUUID();

    @Test
    void createGeneratesUuidV7Id() {
        InventoryItem item = InventoryItem.create(PRODUCT_ID, 5);

        assertThat(item.getId().version()).isEqualTo(7);
        assertThat(Instant.ofEpochMilli(item.getId().getMostSignificantBits() >>> 16))
                .isCloseTo(Instant.now(), within(1, ChronoUnit.MINUTES));
        assertThat(item.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(item.getQuantityOnHand()).isEqualTo(5);
        assertThat(item.getQuantityReserved()).isZero();
    }

    @Test
    void rejectsNegativeQuantityOnHand() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> InventoryItem.create(PRODUCT_ID, -1))
                .withMessage("Quantity on hand must not be negative");
    }
}
