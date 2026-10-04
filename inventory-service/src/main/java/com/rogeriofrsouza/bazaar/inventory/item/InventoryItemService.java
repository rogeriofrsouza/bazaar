package com.rogeriofrsouza.bazaar.inventory.item;

import io.hypersistence.tsid.TSID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;

import static com.rogeriofrsouza.bazaar.inventory.item.InventoryItemSpecifications.productIdIn;

@Service
public class InventoryItemService {

    private final InventoryItemRepository inventoryItemRepository;

    InventoryItemService(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @Transactional(readOnly = true)
    public InventoryItemResponse findByProductId(Long productId) {
        return inventoryItemRepository.findByProductId(productId)
                .map(InventoryItemResponse::from)
                .orElseThrow(() -> notFound(productId));
    }

    @Transactional(readOnly = true)
    public Page<InventoryItemResponse> findAll(Collection<Long> productIds, Pageable pageable) {
        Specification<InventoryItem> specification = productIdIn(productIds);

        return inventoryItemRepository.findAll(specification, pageable)
                .map(InventoryItemResponse::from);
    }

    @Transactional
    public InventoryItemResponse create(CreateInventoryItemRequest request) {
        Long productId = request.productId().toLong();

        if (inventoryItemRepository.existsByProductId(productId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Inventory item " + request.productId() + " already exists");
        }

        InventoryItem item = InventoryItem.create(productId, request.quantityOnHand());
        return InventoryItemResponse.from(inventoryItemRepository.save(item));
    }

    @Transactional
    public InventoryItemResponse restock(Long productId, int quantity) {
        InventoryItem item = findForUpdate(productId);
        item.restock(quantity);
        return InventoryItemResponse.from(item);
    }

    @Transactional
    public InventoryItemResponse reserve(Long productId, int quantity) {
        InventoryItem item = findForUpdate(productId);
        item.reserve(quantity);
        return InventoryItemResponse.from(item);
    }

    @Transactional
    public InventoryItemResponse release(Long productId, int quantity) {
        InventoryItem item = findForUpdate(productId);
        item.release(quantity);
        return InventoryItemResponse.from(item);
    }

    @Transactional
    public InventoryItemResponse fulfil(Long productId, int quantity) {
        InventoryItem item = findForUpdate(productId);
        item.fulfil(quantity);
        return InventoryItemResponse.from(item);
    }

    private InventoryItem findForUpdate(Long productId) {
        return inventoryItemRepository.findWithLockByProductId(productId)
                .orElseThrow(() -> notFound(productId));
    }

    private static ResponseStatusException notFound(Long productId) {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Inventory item " + TSID.from(productId) + " not found");
    }
}
