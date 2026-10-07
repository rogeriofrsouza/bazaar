package com.rogeriofrsouza.bazaar.inventory.item;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.UUID;

import static com.rogeriofrsouza.bazaar.inventory.item.InventoryItemSpecifications.productIdIn;

@Service
public class InventoryItemService {

    private final InventoryItemRepository inventoryItemRepository;

    InventoryItemService(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @Transactional(readOnly = true)
    public InventoryItemResponse findByProductId(UUID productId) {
        return inventoryItemRepository.findByProductId(productId)
                .map(InventoryItemResponse::from)
                .orElseThrow(() -> notFound(productId));
    }

    @Transactional(readOnly = true)
    public Page<InventoryItemResponse> findAll(Collection<UUID> productIds, Pageable pageable) {
        Specification<InventoryItem> specification = productIdIn(productIds);

        return inventoryItemRepository.findAll(specification, pageable)
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
        return InventoryItemResponse.from(item);
    }

    @Transactional
    public InventoryItemResponse reserve(UUID productId, int quantity) {
        InventoryItem item = findForUpdate(productId);
        item.reserve(quantity);
        return InventoryItemResponse.from(item);
    }

    @Transactional
    public InventoryItemResponse release(UUID productId, int quantity) {
        InventoryItem item = findForUpdate(productId);
        item.release(quantity);
        return InventoryItemResponse.from(item);
    }

    @Transactional
    public InventoryItemResponse fulfil(UUID productId, int quantity) {
        InventoryItem item = findForUpdate(productId);
        item.fulfil(quantity);
        return InventoryItemResponse.from(item);
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
