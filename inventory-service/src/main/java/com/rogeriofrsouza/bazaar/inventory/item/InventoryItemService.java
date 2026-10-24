package com.rogeriofrsouza.bazaar.inventory.item;

import com.rogeriofrsouza.bazaar.inventory.item.ReservationRequest.ReservationItemRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.function.ObjIntConsumer;
import java.util.stream.Collectors;

import static com.rogeriofrsouza.bazaar.inventory.item.InventoryItemCriteria.productIdIn;

@Service
public class InventoryItemService {

    private final InventoryItemRepository inventoryItemRepository;
    private final JdbcAggregateOperations jdbcAggregateOperations;

    InventoryItemService(InventoryItemRepository inventoryItemRepository,
                         JdbcAggregateOperations jdbcAggregateOperations) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.jdbcAggregateOperations = jdbcAggregateOperations;
    }

    @Transactional(readOnly = true)
    public InventoryItemResponse findByProductId(UUID productId) {
        return inventoryItemRepository.findByProductId(productId)
                .map(InventoryItemResponse::from)
                .orElseThrow(() -> notFound(productId));
    }

    @Transactional(readOnly = true)
    public Page<InventoryItemResponse> findAll(@Nullable Collection<UUID> productIds, Pageable pageable) {
        var query = Query.query(productIdIn(productIds));
        List<InventoryItem> items = jdbcAggregateOperations.findAll(query.with(pageable), InventoryItem.class);

        return PageableExecutionUtils.getPage(
                        items, pageable, () -> jdbcAggregateOperations.count(query, InventoryItem.class))
                .map(InventoryItemResponse::from);
    }

    @Transactional
    public InventoryItemResponse create(CreateInventoryItemRequest request) {
        UUID productId = request.productId();

        if (inventoryItemRepository.existsByProductId(productId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Inventory item " + request.productId() + " already exists");
        }

        InventoryItem item = InventoryItem.create(productId, request.quantityOnHand());
        return InventoryItemResponse.from(inventoryItemRepository.save(item));
    }

    @Transactional
    public InventoryItemResponse restock(UUID productId, int quantity) {
        InventoryItem item = findForUpdate(productId);
        item.restock(quantity);
        return InventoryItemResponse.from(inventoryItemRepository.save(item));
    }

    @Transactional
    public InventoryItemResponse reserve(UUID productId, int quantity) {
        InventoryItem item = findForUpdate(productId);
        item.reserve(quantity);
        return InventoryItemResponse.from(inventoryItemRepository.save(item));
    }

    @Transactional
    public InventoryItemResponse release(UUID productId, int quantity) {
        InventoryItem item = findForUpdate(productId);
        item.release(quantity);
        return InventoryItemResponse.from(inventoryItemRepository.save(item));
    }

    @Transactional
    public InventoryItemResponse fulfil(UUID productId, int quantity) {
        InventoryItem item = findForUpdate(productId);
        item.fulfil(quantity);
        return InventoryItemResponse.from(inventoryItemRepository.save(item));
    }

    @Transactional
    public List<InventoryItemResponse> reserveAll(ReservationRequest request) {
        return changeAll(request, InventoryItem::reserve);
    }

    @Transactional
    public List<InventoryItemResponse> releaseAll(ReservationRequest request) {
        return changeAll(request, InventoryItem::release);
    }

    @Transactional
    public List<InventoryItemResponse> fulfilAll(ReservationRequest request) {
        return changeAll(request, InventoryItem::fulfil);
    }

    private List<InventoryItemResponse> changeAll(ReservationRequest request,
                                                  ObjIntConsumer<InventoryItem> inventoryModifier) {
        Map<UUID, Integer> quantities = request.items()
                .stream()
                .collect(Collectors.toMap(ReservationItemRequest::productId, ReservationItemRequest::quantity, Integer::sum));

        List<InventoryItem> items =
                inventoryItemRepository.findAllWithLockByProductIdInOrderByProductId(quantities.keySet());

        if (items.size() < quantities.size()) {
            Set<UUID> found = items.stream()
                    .map(InventoryItem::getProductId)
                    .collect(Collectors.toSet());

            List<String> missing = quantities.keySet()
                    .stream()
                    .filter(id -> !found.contains(id))
                    .sorted()
                    .map(UUID::toString)
                    .toList();
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Inventory items not found: " + String.join(", ", missing));
        }

        items.forEach(item -> inventoryModifier.accept(item, quantities.get(item.getProductId())));

        return inventoryItemRepository.saveAll(items)
                .stream()
                .map(InventoryItemResponse::from)
                .toList();
    }

    private InventoryItem findForUpdate(UUID productId) {
        return inventoryItemRepository.findWithLockByProductId(productId)
                .orElseThrow(() -> notFound(productId));
    }

    private static ResponseStatusException notFound(UUID productId) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Inventory item " + productId + " not found");
    }
}
