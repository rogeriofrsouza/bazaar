package com.rogeriofrsouza.bazaar.order.order;

import com.rogeriofrsouza.bazaar.order.catalog.CatalogClient;
import com.rogeriofrsouza.bazaar.order.catalog.CatalogProduct;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderNumberGenerator orderNumberGenerator;
    private final CatalogClient catalogClient;

    OrderService(OrderRepository orderRepository, OrderNumberGenerator orderNumberGenerator,
                 CatalogClient catalogClient) {
        this.orderRepository = orderRepository;
        this.orderNumberGenerator = orderNumberGenerator;
        this.catalogClient = catalogClient;
    }

    @Transactional(readOnly = true)
    public OrderResponse findByNumber(String number) {
        return orderRepository.findByNumber(number.toUpperCase())
                .map(OrderResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order " + number + " not found"));
    }

    @Transactional
    public OrderResponse place(CreateOrderRequest request) {
        List<OrderItem> items = new ArrayList<>();
        Currency currency = null;

        for (OrderItemRequest itemRequest : request.items()) {
            CatalogProduct product = catalogClient.getProduct(itemRequest.productCode());

            if (currency == null) {
                currency = product.currency();
            } else if (!currency.equals(product.currency())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "All products in an order must share one currency");
            }

            items.add(OrderItem.create(product.code(), product.name(), product.price(), itemRequest.quantity()));
        }

        Order order = Order.place(orderNumberGenerator.generate(), currency, items);
        return OrderResponse.from(orderRepository.save(order));
    }
}
