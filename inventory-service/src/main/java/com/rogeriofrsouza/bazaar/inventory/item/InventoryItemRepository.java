package com.rogeriofrsouza.bazaar.inventory.item;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    Optional<InventoryItem> findByProductCode(String productCode);

    List<InventoryItem> findByProductCodeInOrderByProductCode(Collection<String> productCodes);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InventoryItem> findWithLockByProductCode(String productCode);

    boolean existsByProductCode(String productCode);
}
