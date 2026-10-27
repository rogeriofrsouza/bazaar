package com.rogeriofrsouza.bazaar.inventory.reservation;

import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class StockReservation {

    @Id
    private final UUID id;

    private final UUID orderId;

    private StockReservationStatus status;

    @MappedCollection(idColumn = "reservation_id")
    private final Set<StockReservationItem> items;

    @Version
    private @Nullable Long version;

    @CreatedDate
    private @Nullable Instant createdAt;

    @LastModifiedDate
    private @Nullable Instant updatedAt;

    public StockReservation(
            UUID id,
            UUID orderId,
            StockReservationStatus status,
            Set<StockReservationItem> items,
            @Nullable Instant createdAt,
            @Nullable Instant updatedAt
    ) {
        Assert.notEmpty(items, "A reservation needs at least one item");
        this.id = id;
        this.orderId = orderId;
        this.status = status;
        this.items = Set.copyOf(items);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static StockReservation reserved(UUID orderId, Map<UUID, Integer> quantities) {
        return create(orderId, StockReservationStatus.RESERVED, quantities);
    }

    public static StockReservation rejected(UUID orderId, Map<UUID, Integer> quantities) {
        return create(orderId, StockReservationStatus.REJECTED, quantities);
    }

    private static StockReservation create(UUID orderId, StockReservationStatus status,
                                           Map<UUID, Integer> quantities) {
        Set<StockReservationItem> items = quantities.entrySet()
                .stream()
                .map(entry -> new StockReservationItem(entry.getKey(), entry.getValue()))
                .collect(Collectors.toSet());

        return new StockReservation(
                UUID.ofEpochMillis(System.currentTimeMillis()),
                orderId,
                status,
                items,
                null,
                null
        );
    }

    public void release() {
        requireReserved();
        status = StockReservationStatus.RELEASED;
    }

    public void fulfil() {
        requireReserved();
        status = StockReservationStatus.FULFILLED;
    }

    private void requireReserved() {
        if (status != StockReservationStatus.RESERVED) {
            throw new IllegalStateException("Reservation for order " + orderId + " is " + status + ", not RESERVED");
        }
    }

    public Map<UUID, Integer> quantities() {
        return items.stream()
                .collect(Collectors.toMap(StockReservationItem::getProductId, StockReservationItem::getQuantity));
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public StockReservationStatus getStatus() {
        return status;
    }

    public Set<StockReservationItem> getItems() {
        return items;
    }

    public @Nullable Instant getCreatedAt() {
        return createdAt;
    }

    public @Nullable Instant getUpdatedAt() {
        return updatedAt;
    }
}
