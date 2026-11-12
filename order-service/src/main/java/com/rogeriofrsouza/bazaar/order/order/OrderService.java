package com.rogeriofrsouza.bazaar.order.order;

import com.rogeriofrsouza.bazaar.order.catalog.CatalogClient;
import com.rogeriofrsouza.bazaar.order.catalog.CatalogProduct;
import com.rogeriofrsouza.bazaar.order.outbox.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final int EXPIRY_BATCH_SIZE = 100;

    private final OrderRepository orderRepository;
    private final CatalogClient catalogClient;
    private final Outbox outbox;
    private final Duration reservationTtl;

    OrderService(OrderRepository orderRepository, CatalogClient catalogClient, Outbox outbox,
                 @Value("${bazaar.orders.reservation-ttl}") Duration reservationTtl) {
        this.orderRepository = orderRepository;
        this.catalogClient = catalogClient;
        this.outbox = outbox;
        this.reservationTtl = reservationTtl;
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(UUID id, String customerSubject) {
        return orderRepository.findByIdAndCustomerSubject(id, customerSubject)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> findAll(String customerSubject, Pageable pageable) {
        return orderRepository.findByCustomerSubject(customerSubject, pageable)
                .map(OrderResponse::from);
    }

    @Transactional
    public OrderResponse place(String customerSubject, CreateOrderRequest request) {
        Set<UUID> productIds = request.items()
                .stream()
                .map(OrderItemRequest::productId)
                .collect(Collectors.toSet());

        Map<UUID, CatalogProduct> products = catalogClient.getProducts(productIds)
                .stream()
                .collect(Collectors.toMap(CatalogProduct::id, Function.identity()));

        if (products.size() < productIds.size()) {
            List<String> missing = productIds.stream()
                    .filter(id -> !products.containsKey(id))
                    .sorted()
                    .map(UUID::toString)
                    .toList();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Products not found: " + String.join(", ", missing));
        }

        List<OrderItem> items = request.items()
                .stream()
                .map(item -> {
                    CatalogProduct product = products.get(item.productId());
                    return OrderItem.create(product.id(), product.name(), product.price(), item.quantity());
                })
                .toList();

        Order order = orderRepository.save(Order.place(customerSubject, items));
        outbox.add(OrdersExchange.PLACED, OrderPlacedMessage.from(order));
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse confirm(UUID id, String customerSubject) {
        Order order = findForUpdate(id, customerSubject);
        order.confirm();
        Order saved = orderRepository.save(order);
        outbox.add(OrdersExchange.CONFIRMED, new OrderConfirmedMessage(id));
        return OrderResponse.from(saved);
    }

    @Transactional
    public OrderResponse cancel(UUID id, String customerSubject) {
        Order order = findForUpdate(id, customerSubject);
        order.cancel();
        Order saved = orderRepository.save(order);
        outbox.add(OrdersExchange.CANCELLED, new OrderCancelledMessage(id));
        return OrderResponse.from(saved);
    }

    @Transactional
    public void markReserved(StockReservedMessage message) {
        changePending(message.orderId(), StockReservedMessage.class,
                order -> order.reserve(Instant.now().plus(reservationTtl)));
    }

    @Transactional
    public void markRejected(StockRejectedMessage message) {
        changePending(message.orderId(), StockRejectedMessage.class, order -> order.reject(message.reason()));
    }

    @Transactional
    public void expireOverdueOrders() {
        Instant now = Instant.now();
        List<Order> overdue = orderRepository.findExpiredForUpdate(now, EXPIRY_BATCH_SIZE);
        overdue.forEach(order -> order.expire(now));
        orderRepository.saveAll(overdue);
        overdue.forEach(order -> outbox.add(OrdersExchange.EXPIRED, new OrderExpiredMessage(order.getId())));
    }

    private void changePending(UUID orderId, Class<?> messageType, Consumer<Order> change) {
        Optional<Order> pending = orderRepository.findWithLockById(orderId)
                .filter(order -> order.getStatus() == OrderStatus.PENDING);

        if (pending.isEmpty()) {
            log.info("Ignoring {} for order {} that is not pending", messageType.getSimpleName(), orderId);
            return;
        }

        Order order = pending.get();
        change.accept(order);
        orderRepository.save(order);
    }

    private Order findForUpdate(UUID id, String customerSubject) {
        return orderRepository.findWithLockByIdAndCustomerSubject(id, customerSubject)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }
}
