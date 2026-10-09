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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@Import(ContainersConfig.class)
@Transactional
class OrderPersistenceTests {

    private static final Currency BRL = Currency.getInstance("BRL");

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void savesAndLoadsOrderWithItems() {
        UUID keyboard = UUID.randomUUID();
        UUID mouse = UUID.randomUUID();
        Order placed = orderRepository.save(Order.place(BRL, List.of(
                OrderItem.create(keyboard, "Keyboard", new BigDecimal("150.00"), 2),
                OrderItem.create(mouse, "Mouse", new BigDecimal("49.90"), 1)
        )));

        Order order = orderRepository.findById(placed.getId()).orElseThrow();

        assertThat(order.getId().version()).isEqualTo(7);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getCurrency()).isEqualTo(BRL);
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
        Order placed = orderRepository.save(Order.place(BRL, List.of(
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

    private Long versionOf(UUID orderId) {
        return jdbcTemplate.queryForObject("SELECT version FROM orders WHERE id = ?", Long.class, orderId);
    }
}
