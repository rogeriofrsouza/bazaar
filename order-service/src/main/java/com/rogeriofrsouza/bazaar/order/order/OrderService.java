package com.rogeriofrsouza.bazaar.order.order;

import com.rogeriofrsouza.bazaar.order.catalog.CatalogClient;
import com.rogeriofrsouza.bazaar.order.catalog.CatalogProduct;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Currency;
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

    OrderService(OrderRepository orderRepository, CatalogClient catalogClient) {
        this.orderRepository = orderRepository;
        this.catalogClient = catalogClient;
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(UUID id) {
        return orderRepository.findWithItemsById(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Order " + id + " not found"));
    }

    @Transactional
    public OrderResponse place(CreateOrderRequest request) {
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

        Set<Currency> currencies = products.values().stream()
                .map(CatalogProduct::currency)
                .collect(Collectors.toSet());

        if (currencies.size() > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "All products in an order must share one currency");
        }

        List<OrderItem> items = request.items()
                .stream()
                .map(item -> {
                    CatalogProduct product = products.get(item.productId());
                    return OrderItem.create(product.id(), product.name(), product.price(), item.quantity());
                })
                .toList();

        Order order = Order.place(currencies.iterator().next(), items);
        return OrderResponse.from(orderRepository.save(order));
    }
}
