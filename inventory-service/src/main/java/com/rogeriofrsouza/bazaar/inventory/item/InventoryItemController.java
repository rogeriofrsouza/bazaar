package com.rogeriofrsouza.bazaar.inventory.item;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RequestMapping("/api/inventory")
@RestController
class InventoryItemController {

    private final InventoryItemService inventoryItemService;

    InventoryItemController(InventoryItemService inventoryItemService) {
        this.inventoryItemService = inventoryItemService;
    }

    @GetMapping("/{code}")
    InventoryItemResponse get(@PathVariable String code) {
        return inventoryItemService.findByProductCode(code);
    }

    @PostMapping
    ResponseEntity<InventoryItemResponse> create(@Valid @RequestBody CreateInventoryItemRequest request) {
        InventoryItemResponse item = inventoryItemService.create(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{code}")
                .buildAndExpand(item.productCode())
                .toUri();

        return ResponseEntity.created(location).body(item);
    }

    @PostMapping("/{code}/restock")
    InventoryItemResponse restock(@PathVariable String code,
                                  @Valid @RequestBody QuantityRequest request) {
        return inventoryItemService.restock(code, request.quantity());
    }

    @PostMapping("/{code}/reserve")
    InventoryItemResponse reserve(@PathVariable String code,
                                  @Valid @RequestBody QuantityRequest request) {
        return inventoryItemService.reserve(code, request.quantity());
    }

    @PostMapping("/{code}/release")
    InventoryItemResponse release(@PathVariable String code,
                                  @Valid @RequestBody QuantityRequest request) {
        return inventoryItemService.release(code, request.quantity());
    }

    @PostMapping("/{code}/fulfil")
    InventoryItemResponse fulfil(@PathVariable String code,
                                 @Valid @RequestBody QuantityRequest request) {
        return inventoryItemService.fulfil(code, request.quantity());
    }
}
