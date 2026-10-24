package com.rogeriofrsouza.bazaar.order.order;

import com.rogeriofrsouza.bazaar.order.catalog.CatalogClient;
import com.rogeriofrsouza.bazaar.order.catalog.CatalogProduct;
import com.rogeriofrsouza.bazaar.order.inventory.InventoryClient;
import com.rogeriofrsouza.bazaar.order.inventory.ReservationRequest;
import com.rogeriofrsouza.bazaar.order.inventory.ReservationRequest.ReservationItemRequest;
import com.rogeriofrsouza.bazaar.order.inventory.StockReservedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CatalogClient catalogClient;
    private final InventoryClient inventoryClient;
    private final ApplicationEventPublisher eventPublisher;

    OrderService(OrderRepository orderRepository, CatalogClient catalogClient, InventoryClient inventoryClient,
                 ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.catalogClient = catalogClient;
        this.inventoryClient = inventoryClient;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(UUID id, String customerSubject) {
        return orderRepository.findByIdAndCustomerSubject(id, customerSubject)
                .map(OrderResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Order " + id + " not found"));
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

        ReservationRequest reservation = new ReservationRequest(
                items.stream()
                        .map(item -> new ReservationItemRequest(item.getProductId(), item.getQuantity()))
                        .toList());

        inventoryClient.reserve(reservation);
        eventPublisher.publishEvent(new StockReservedEvent(reservation));

        Order order = Order.place(customerSubject, items);
        return OrderResponse.from(orderRepository.save(order));
    }
}
