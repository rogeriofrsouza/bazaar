package com.rogeriofrsouza.bazaar.inventory.reservation;

import com.rogeriofrsouza.bazaar.inventory.item.InsufficientStockException;
import com.rogeriofrsouza.bazaar.inventory.item.InventoryItem;
import com.rogeriofrsouza.bazaar.inventory.item.InventoryItemRepository;
import com.rogeriofrsouza.bazaar.inventory.outbox.Outbox;
import com.rogeriofrsouza.bazaar.inventory.reservation.OrderPlacedMessage.OrderPlacedItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;
import java.util.stream.Collectors;

@Service
public class StockReservationService {

    private static final Logger log = LoggerFactory.getLogger(StockReservationService.class);

    private final StockReservationRepository stockReservationRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final Outbox outbox;

    StockReservationService(StockReservationRepository stockReservationRepository,
                            InventoryItemRepository inventoryItemRepository,
                            Outbox outbox) {
        this.stockReservationRepository = stockReservationRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.outbox = outbox;
    }

    @Transactional
    public void reserve(OrderPlacedMessage message) {
        UUID orderId = message.orderId();

        if (stockReservationRepository.existsByOrderId(orderId)) {
            log.info("Ignoring duplicate {} for order {}", OrderPlacedMessage.ROUTING_KEY, orderId);
            return;
        }

        Map<UUID, Integer> quantities = message.items()
                .stream()
                .collect(Collectors.toMap(OrderPlacedItem::productId, OrderPlacedItem::quantity, Integer::sum));

        List<InventoryItem> items =
                inventoryItemRepository.findAllWithLockByProductIdInOrderByProductId(quantities.keySet());

        List<String> missing = missingProducts(quantities, items);
        if (!missing.isEmpty()) {
            reject(orderId, quantities, "Inventory items not found: " + String.join(", ", missing));
            return;
        }

        try {
            items.forEach(item -> item.reserve(quantities.get(item.getProductId())));
        } catch (InsufficientStockException ex) {
            reject(orderId, quantities, ex.getMessage());
            return;
        }

        inventoryItemRepository.saveAll(items);
        stockReservationRepository.save(StockReservation.reserved(orderId, quantities));
        outbox.add(StockReservedMessage.ROUTING_KEY, new StockReservedMessage(orderId));
    }

    @Transactional
    public void fulfil(OrderConfirmedMessage message) {
        change(message.orderId(), OrderConfirmedMessage.ROUTING_KEY, InventoryItem::fulfil, StockReservation::fulfil);
    }

    @Transactional
    public void release(OrderCancelledMessage message) {
        change(message.orderId(), OrderCancelledMessage.ROUTING_KEY, InventoryItem::release, StockReservation::release);
    }

    private void reject(UUID orderId, Map<UUID, Integer> quantities, String reason) {
        stockReservationRepository.save(StockReservation.rejected(orderId, quantities));
        outbox.add(StockRejectedMessage.ROUTING_KEY, new StockRejectedMessage(orderId, reason));
    }

    private void change(UUID orderId, String routingKey, ObjIntConsumer<InventoryItem> itemChange,
                        Consumer<StockReservation> reservationChange) {
        Optional<StockReservation> reserved = stockReservationRepository.findWithLockByOrderId(orderId)
                .filter(reservation -> reservation.getStatus() == StockReservationStatus.RESERVED);

        if (reserved.isEmpty()) {
            log.info("Ignoring {} for order {} with no reserved stock", routingKey, orderId);
            return;
        }

        StockReservation reservation = reserved.get();
        Map<UUID, Integer> quantities = reservation.quantities();
        List<InventoryItem> items =
                inventoryItemRepository.findAllWithLockByProductIdInOrderByProductId(quantities.keySet());

        List<String> missing = missingProducts(quantities, items);
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Inventory items of order " + orderId + " not found: "
                    + String.join(", ", missing));
        }

        items.forEach(item -> itemChange.accept(item, quantities.get(item.getProductId())));
        inventoryItemRepository.saveAll(items);
        reservationChange.accept(reservation);
        stockReservationRepository.save(reservation);
    }

    private static List<String> missingProducts(Map<UUID, Integer> quantities, List<InventoryItem> items) {
        Set<UUID> found = items.stream()
                .map(InventoryItem::getProductId)
                .collect(Collectors.toSet());

        return quantities.keySet()
                .stream()
                .filter(id -> !found.contains(id))
                .sorted()
                .map(UUID::toString)
                .toList();
    }
}
