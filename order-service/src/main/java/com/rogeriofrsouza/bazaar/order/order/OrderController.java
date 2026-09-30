package com.rogeriofrsouza.bazaar.order.order;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RequestMapping("/api/orders")
@RestController
class OrderController {

    private final OrderService orderService;

    OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{number}")
    OrderResponse get(@PathVariable String number) {
        return orderService.findByNumber(number);
    }

    @PostMapping
    ResponseEntity<OrderResponse> place(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse order = orderService.place(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{number}")
                .buildAndExpand(order.number())
                .toUri();

        return ResponseEntity.created(location).body(order);
    }
}
