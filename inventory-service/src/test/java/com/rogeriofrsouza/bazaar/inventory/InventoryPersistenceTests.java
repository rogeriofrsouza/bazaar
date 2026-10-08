package com.rogeriofrsouza.bazaar.inventory;

import com.rogeriofrsouza.bazaar.inventory.item.CreateInventoryItemRequest;
import com.rogeriofrsouza.bazaar.inventory.item.InsufficientStockException;
import com.rogeriofrsouza.bazaar.inventory.item.InventoryItem;
import com.rogeriofrsouza.bazaar.inventory.item.InventoryItemRepository;
import com.rogeriofrsouza.bazaar.inventory.item.InventoryItemResponse;
import com.rogeriofrsouza.bazaar.inventory.item.InventoryItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@SpringBootTest
@Import(ContainersConfig.class)
@Transactional
class InventoryPersistenceTests {

    private static final PageRequest PAGE = PageRequest.of(0, 20, Sort.by("productId"));

    @Autowired
    private InventoryItemService inventoryItemService;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsAndReadsItem() {
        UUID productId = UUID.randomUUID();

        InventoryItemResponse created = inventoryItemService.create(new CreateInventoryItemRequest(productId, 10));
        assertThat(created).isEqualTo(new InventoryItemResponse(productId, 10, 0, 10));
        assertThat(inventoryItemService.findByProductId(productId)).isEqualTo(created);

        InventoryItem item = inventoryItemRepository.findByProductId(productId).orElseThrow();
        assertThat(item.getId().version()).isEqualTo(7);
        assertThat(item.getCreatedAt()).isNotNull();
        assertThat(item.getUpdatedAt()).isNotNull();
        assertThat(versionOf(productId)).isZero();
    }

    @Test
    void rejectsDuplicateItem() {
        UUID productId = UUID.randomUUID();
        inventoryItemService.create(new CreateInventoryItemRequest(productId, 10));

        assertThatExceptionOfType(ResponseStatusException.class)
                .isThrownBy(() -> inventoryItemService.create(new CreateInventoryItemRequest(productId, 5)))
                .satisfies(ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void persistsStockChanges() {
        UUID productId = UUID.randomUUID();
        inventoryItemService.create(new CreateInventoryItemRequest(productId, 10));
        InventoryItem created = inventoryItemRepository.findByProductId(productId).orElseThrow();

        inventoryItemService.restock(productId, 5);
        inventoryItemService.reserve(productId, 8);
        inventoryItemService.release(productId, 2);
        inventoryItemService.fulfil(productId, 4);

        InventoryItem item = inventoryItemRepository.findByProductId(productId).orElseThrow();
        assertThat(item.getQuantityOnHand()).isEqualTo(11);
        assertThat(item.getQuantityReserved()).isEqualTo(2);
        assertThat(item.getAvailable()).isEqualTo(9);
        assertThat(item.getCreatedAt()).isEqualTo(created.getCreatedAt());
        assertThat(item.getUpdatedAt()).isAfterOrEqualTo(created.getUpdatedAt());
        assertThat(versionOf(productId)).isEqualTo(4);
        assertThat(inventoryItemService.findByProductId(productId))
                .isEqualTo(new InventoryItemResponse(productId, 11, 2, 9));
    }

    @Test
    void leavesStockUnchangedWhenReservationFails() {
        UUID productId = UUID.randomUUID();
        inventoryItemService.create(new CreateInventoryItemRequest(productId, 3));

        assertThatExceptionOfType(InsufficientStockException.class)
                .isThrownBy(() -> inventoryItemService.reserve(productId, 4));

        assertThat(inventoryItemService.findByProductId(productId))
                .isEqualTo(new InventoryItemResponse(productId, 3, 0, 3));
        assertThat(versionOf(productId)).isZero();
    }

    @Test
    void filtersItemsByProductIds() {
        InventoryItemResponse first = inventoryItemService.create(request(1));
        InventoryItemResponse second = inventoryItemService.create(request(2));

        Page<InventoryItemResponse> all = inventoryItemService.findAll(null, PAGE);
        assertThat(all.getContent()).contains(first, second);

        Page<InventoryItemResponse> byIds = inventoryItemService.findAll(List.of(second.productId()), PAGE);
        assertThat(byIds.getContent()).containsExactly(second);
        assertThat(byIds.getTotalElements()).isEqualTo(1);
    }

    private static CreateInventoryItemRequest request(int quantityOnHand) {
        return new CreateInventoryItemRequest(UUID.randomUUID(), quantityOnHand);
    }

    private Long versionOf(UUID productId) {
        return jdbcTemplate.queryForObject(
                "SELECT version FROM inventory_item WHERE product_id = ?", Long.class, productId);
    }
}
