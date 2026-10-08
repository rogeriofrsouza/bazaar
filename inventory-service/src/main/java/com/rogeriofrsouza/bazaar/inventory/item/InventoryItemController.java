package com.rogeriofrsouza.bazaar.inventory.item;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RequestMapping("/api/inventory")
@RestController
class InventoryItemController {

    private final InventoryItemService inventoryItemService;

    InventoryItemController(InventoryItemService inventoryItemService) {
        this.inventoryItemService = inventoryItemService;
    }

    @GetMapping("/{productId}")
    InventoryItemResponse get(@PathVariable UUID productId) {
        return inventoryItemService.findByProductId(productId);
    }

    @GetMapping
    PagedModel<InventoryItemResponse> list(
            @RequestParam(required = false) @Size(max = 20) @Nullable List<UUID> productIds,
            @ParameterObject @PageableDefault(size = 20, sort = "productId") Pageable pageable) {
        Page<InventoryItemResponse> page = inventoryItemService.findAll(productIds, pageable);
        return new PagedModel<>(page);
    }

    @PostMapping
    ResponseEntity<InventoryItemResponse> create(@Valid @RequestBody CreateInventoryItemRequest request) {
        InventoryItemResponse item = inventoryItemService.create(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{productId}")
                .buildAndExpand(item.productId())
                .toUri();

        return ResponseEntity.created(location).body(item);
    }

    @PostMapping("/{productId}/restock")
    InventoryItemResponse restock(@PathVariable UUID productId,
                                  @Valid @RequestBody QuantityRequest request) {
        return inventoryItemService.restock(productId, request.quantity());
    }

    @PostMapping("/{productId}/reserve")
    InventoryItemResponse reserve(@PathVariable UUID productId,
                                  @Valid @RequestBody QuantityRequest request) {
        return inventoryItemService.reserve(productId, request.quantity());
    }

    @PostMapping("/{productId}/release")
    InventoryItemResponse release(@PathVariable UUID productId,
                                  @Valid @RequestBody QuantityRequest request) {
        return inventoryItemService.release(productId, request.quantity());
    }

    @PostMapping("/{productId}/fulfil")
    InventoryItemResponse fulfil(@PathVariable UUID productId,
                                 @Valid @RequestBody QuantityRequest request) {
        return inventoryItemService.fulfil(productId, request.quantity());
    }
}
