package com.rogeriofrsouza.bazaar.inventory.item;

import io.hypersistence.tsid.TSID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
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

@RequestMapping("/api/inventory")
@RestController
class InventoryItemController {

    private final InventoryItemService inventoryItemService;

    InventoryItemController(InventoryItemService inventoryItemService) {
        this.inventoryItemService = inventoryItemService;
    }

    @GetMapping("/{productId}")
    InventoryItemResponse get(@PathVariable TSID productId) {
        return inventoryItemService.findByProductId(productId.toLong());
    }

    @GetMapping
    PagedModel<InventoryItemResponse> list(@RequestParam(required = false) @Size(max = 20) List<TSID> productIds,
                                           @ParameterObject @PageableDefault(size = 20, sort = "productId") Pageable pageable) {
        List<Long> ids = productIds == null ? null : productIds.stream().map(TSID::toLong).toList();
        Page<InventoryItemResponse> page = inventoryItemService.findAll(ids, pageable);
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
    InventoryItemResponse restock(@PathVariable TSID productId,
                                  @Valid @RequestBody QuantityRequest request) {
        return inventoryItemService.restock(productId.toLong(), request.quantity());
    }

    @PostMapping("/{productId}/reserve")
    InventoryItemResponse reserve(@PathVariable TSID productId,
                                  @Valid @RequestBody QuantityRequest request) {
        return inventoryItemService.reserve(productId.toLong(), request.quantity());
    }

    @PostMapping("/{productId}/release")
    InventoryItemResponse release(@PathVariable TSID productId,
                                  @Valid @RequestBody QuantityRequest request) {
        return inventoryItemService.release(productId.toLong(), request.quantity());
    }

    @PostMapping("/{productId}/fulfil")
    InventoryItemResponse fulfil(@PathVariable TSID productId,
                                 @Valid @RequestBody QuantityRequest request) {
        return inventoryItemService.fulfil(productId.toLong(), request.quantity());
    }
}
