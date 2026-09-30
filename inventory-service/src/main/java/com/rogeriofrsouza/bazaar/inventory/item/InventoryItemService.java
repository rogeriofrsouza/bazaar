package com.rogeriofrsouza.bazaar.inventory.item;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;

import static com.rogeriofrsouza.bazaar.inventory.item.InventoryItemSpecifications.productCodeIn;

@Service
public class InventoryItemService {

    private final InventoryItemRepository inventoryItemRepository;

    InventoryItemService(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @Transactional(readOnly = true)
    public InventoryItemResponse findByProductCode(String code) {
        return inventoryItemRepository.findByProductCode(code)
                .map(InventoryItemResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory item " + code + " not found"));
    }

    @Transactional(readOnly = true)
    public Page<InventoryItemResponse> findAll(Collection<String> codes, Pageable pageable) {
        Specification<InventoryItem> specification = productCodeIn(codes);

        return inventoryItemRepository.findAll(specification, pageable)
                .map(InventoryItemResponse::from);
    }

    @Transactional
    public InventoryItemResponse create(CreateInventoryItemRequest request) {
        String code = request.productCode();
        if (inventoryItemRepository.existsByProductCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Inventory item " + code + " already exists");
        }

        InventoryItem item = InventoryItem.create(code, request.quantityOnHand());
        return InventoryItemResponse.from(inventoryItemRepository.save(item));
    }

    @Transactional
    public InventoryItemResponse restock(String code, int quantity) {
        InventoryItem item = findForUpdate(code);
        item.restock(quantity);
        return InventoryItemResponse.from(item);
    }

    @Transactional
    public InventoryItemResponse reserve(String code, int quantity) {
        InventoryItem item = findForUpdate(code);
        item.reserve(quantity);
        return InventoryItemResponse.from(item);
    }

    @Transactional
    public InventoryItemResponse release(String code, int quantity) {
        InventoryItem item = findForUpdate(code);
        item.release(quantity);
        return InventoryItemResponse.from(item);
    }

    @Transactional
    public InventoryItemResponse fulfil(String code, int quantity) {
        InventoryItem item = findForUpdate(code);
        item.fulfil(quantity);
        return InventoryItemResponse.from(item);
    }

    private InventoryItem findForUpdate(String code) {
        return inventoryItemRepository.findWithLockByProductCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory item " + code + " not found"));
    }
}
