package com.rogeriofrsouza.bazaar.order;

import com.rogeriofrsouza.bazaar.order.order.Order;
import com.rogeriofrsouza.bazaar.order.order.OrderItem;
import com.rogeriofrsouza.bazaar.order.order.OrderRepository;
import com.rogeriofrsouza.bazaar.order.order.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@Import(ContainersConfig.class)
@Transactional
class OrderPersistenceTests {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void savesAndLoadsOrderWithItems() {
        UUID keyboard = UUID.randomUUID();
        UUID mouse = UUID.randomUUID();
        Order placed = orderRepository.save(Order.place("subject-1", List.of(
                OrderItem.create(keyboard, "Keyboard", new BigDecimal("150.00"), 2),
                OrderItem.create(mouse, "Mouse", new BigDecimal("49.90"), 1)
        )));

        Order order = orderRepository.findById(placed.getId()).orElseThrow();

        assertThat(order.getId().version()).isEqualTo(7);
        assertThat(order.getCustomerSubject()).isEqualTo("subject-1");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTotal()).isEqualByComparingTo("349.90");
        assertThat(order.getCreatedAt()).isNotNull();
        assertThat(order.getUpdatedAt()).isNotNull();
        assertThat(order.getItems())
                .extracting(OrderItem::getProductId, OrderItem::getProductName,
                        OrderItem::getUnitPrice, OrderItem::getQuantity)
                .containsExactlyInAnyOrder(
                        tuple(keyboard, "Keyboard", new BigDecimal("150.00"), 2),
                        tuple(mouse, "Mouse", new BigDecimal("49.90"), 1)
                );
        assertThat(order.getItems()).extracting(OrderItem::getId)
                .containsExactlyInAnyOrderElementsOf(placed.getItems().stream().map(OrderItem::getId).toList())
                .allSatisfy(id -> assertThat(id.version()).isEqualTo(7));
        assertThat(versionOf(order.getId())).isZero();
    }

    @Test
    void resavingLoadedOrderKeepsItems() {
        Order placed = orderRepository.save(Order.place("subject-1", List.of(
                OrderItem.create(UUID.randomUUID(), "Monitor", new BigDecimal("900.00"), 1),
                OrderItem.create(UUID.randomUUID(), "Cable", new BigDecimal("9.99"), 3)
        )));
        Order loaded = orderRepository.findById(placed.getId()).orElseThrow();

        orderRepository.save(loaded);

        Order resaved = orderRepository.findById(placed.getId()).orElseThrow();
        assertThat(resaved.getItems()).extracting(OrderItem::getId)
                .containsExactlyInAnyOrderElementsOf(loaded.getItems().stream().map(OrderItem::getId).toList());
        assertThat(resaved.getCreatedAt()).isEqualTo(loaded.getCreatedAt());
        assertThat(versionOf(placed.getId())).isEqualTo(1);
    }

    @Test
    void persistsStatusChange() {
        Order placed = orderRepository.save(Order.place("subject-1", List.of(
                OrderItem.create(UUID.randomUUID(), "Headset", new BigDecimal("199.00"), 1)
        )));
        Order locked = orderRepository.findWithLockByIdAndCustomerSubject(placed.getId(), "subject-1").orElseThrow();

        Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MICROS);
        locked.reserve(expiresAt);
        orderRepository.save(locked);
        Order reserved = orderRepository.findWithLockById(placed.getId()).orElseThrow();
        assertThat(reserved.getExpiresAt()).isEqualTo(expiresAt);
        reserved.confirm();
        orderRepository.save(reserved);

        Order confirmed = orderRepository.findById(placed.getId()).orElseThrow();
        assertThat(confirmed.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(confirmed.getRejectionReason()).isNull();
        assertThat(confirmed.getItems()).extracting(OrderItem::getProductName).containsExactly("Headset");
        assertThat(versionOf(placed.getId())).isEqualTo(2);
    }

    @Test
    void persistsRejectionReason() {
        Order placed = orderRepository.save(Order.place("subject-1", List.of(
                OrderItem.create(UUID.randomUUID(), "Monitor", new BigDecimal("900.00"), 2)
        )));
        Order locked = orderRepository.findWithLockById(placed.getId()).orElseThrow();

        locked.reject("Only 1 units available");
        orderRepository.save(locked);

        Order rejected = orderRepository.findById(placed.getId()).orElseThrow();
        assertThat(rejected.getStatus()).isEqualTo(OrderStatus.REJECTED);
        assertThat(rejected.getRejectionReason()).isEqualTo("Only 1 units available");
    }

    @Test
    void loadsBoundedBatchOfOverdueOrdersWithItems() {
        Instant now = Instant.parse("2020-01-01T00:00:00Z");
        Order first = overdueOrder(now.minusSeconds(2));
        Order second = overdueOrder(now.minusSeconds(1));

        List<Order> batch = orderRepository.findExpiredForUpdate(now, 1);

        assertThat(batch).extracting(Order::getId).containsExactly(first.getId());
        Order loaded = batch.getFirst();
        assertThat(loaded.getItems()).extracting(OrderItem::getId)
                .containsExactlyInAnyOrderElementsOf(first.getItems().stream().map(OrderItem::getId).toList());

        loaded.expire(now);
        orderRepository.save(loaded);

        Order expired = orderRepository.findById(first.getId()).orElseThrow();
        assertThat(expired.getStatus()).isEqualTo(OrderStatus.EXPIRED);
        assertThat(expired.getItems()).extracting(OrderItem::getId)
                .containsExactlyInAnyOrderElementsOf(first.getItems().stream().map(OrderItem::getId).toList());
        assertThat(versionOf(first.getId())).isEqualTo(1);
        assertThat(orderRepository.findExpiredForUpdate(now, 10))
                .extracting(Order::getId).containsExactly(second.getId());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void skipsOrdersLockedByAnotherTransaction() {
        Instant now = Instant.parse("2020-01-01T00:00:00Z");
        Order first = overdueOrder(now.minusSeconds(2));
        Order second = overdueOrder(now.minusSeconds(1));
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.setTimeout(5);

        try {
            transaction.executeWithoutResult(_ -> {
                List<Order> locked = orderRepository.findExpiredForUpdate(now, 1);
                assertThat(locked).extracting(Order::getId).containsExactly(first.getId());

                transaction.executeWithoutResult(_ ->
                        assertThat(orderRepository.findExpiredForUpdate(now, 10))
                                .extracting(Order::getId).containsExactly(second.getId()));

                Order order = locked.getFirst();
                order.confirm();
                orderRepository.save(order);
            });

            transaction.executeWithoutResult(_ ->
                    assertThat(orderRepository.findExpiredForUpdate(now, 10))
                            .extracting(Order::getId).containsExactly(second.getId()));
        } finally {
            orderRepository.deleteAllById(List.of(first.getId(), second.getId()));
        }
    }

    private Order overdueOrder(Instant expiresAt) {
        Order order = Order.place("subject-1", List.of(
                OrderItem.create(UUID.randomUUID(), "Keyboard", new BigDecimal("150.00"), 2),
                OrderItem.create(UUID.randomUUID(), "Mouse", new BigDecimal("49.90"), 1)
        ));
        order.reserve(expiresAt);
        return orderRepository.save(order);
    }

    private Long versionOf(UUID orderId) {
        return jdbcTemplate.queryForObject("SELECT version FROM orders WHERE id = ?", Long.class, orderId);
    }
}
