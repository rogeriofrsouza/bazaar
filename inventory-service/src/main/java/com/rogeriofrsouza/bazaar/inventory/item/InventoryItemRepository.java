package com.rogeriofrsouza.bazaar.inventory.item;

import org.springframework.data.relational.core.sql.LockMode;
import org.springframework.data.relational.repository.Lock;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryItemRepository extends ListCrudRepository<InventoryItem, UUID>,
        ListPagingAndSortingRepository<InventoryItem, UUID> {

    Optional<InventoryItem> findByProductId(UUID productId);

    @Lock(LockMode.PESSIMISTIC_WRITE)
    Optional<InventoryItem> findWithLockByProductId(UUID productId);

    @Lock(LockMode.PESSIMISTIC_WRITE)
    List<InventoryItem> findAllWithLockByProductIdInOrderByProductId(Collection<UUID> productIds);

    boolean existsByProductId(UUID productId);
}
