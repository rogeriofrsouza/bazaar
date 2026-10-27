package com.rogeriofrsouza.bazaar.inventory.reservation;

import org.springframework.data.relational.core.sql.LockMode;
import org.springframework.data.relational.repository.Lock;
import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface StockReservationRepository extends ListCrudRepository<StockReservation, UUID> {

    Optional<StockReservation> findByOrderId(UUID orderId);

    @Lock(LockMode.PESSIMISTIC_WRITE)
    Optional<StockReservation> findWithLockByOrderId(UUID orderId);

    boolean existsByOrderId(UUID orderId);
}
