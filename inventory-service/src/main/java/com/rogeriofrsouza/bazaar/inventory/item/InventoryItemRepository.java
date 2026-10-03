package com.rogeriofrsouza.bazaar.inventory.item;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long>,
        JpaSpecificationExecutor<InventoryItem> {

    Optional<InventoryItem> findByProductId(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InventoryItem> findWithLockByProductId(Long productId);

    boolean existsByProductId(Long productId);
}
