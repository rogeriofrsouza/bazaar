package com.rogeriofrsouza.bazaar.order.order;

import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RequestMapping("/api/orders")
@RestController
class OrderController {

    private final OrderService orderService;

    OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    PagedModel<OrderResponse> list(@AuthenticationPrincipal Jwt jwt,
                                   @ParameterObject
                                   @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                                   Pageable pageable) {
        Page<OrderResponse> page = orderService.findAll(subjectOf(jwt), pageable);
        return new PagedModel<>(page);
    }

    @GetMapping("/{id}")
    OrderResponse get(@AuthenticationPrincipal Jwt jwt,
                      @PathVariable UUID id) {
        return orderService.findById(id, subjectOf(jwt));
    }

    @PostMapping
    ResponseEntity<OrderResponse> place(@AuthenticationPrincipal Jwt jwt,
                                        @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse order = orderService.place(subjectOf(jwt), request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(order.id())
                .toUri();

        return ResponseEntity.created(location).body(order);
    }

    @PostMapping("/{id}/confirm")
    OrderResponse confirm(@AuthenticationPrincipal Jwt jwt,
                          @PathVariable UUID id) {
        return orderService.confirm(id, subjectOf(jwt));
    }

    @PostMapping("/{id}/cancel")
    OrderResponse cancel(@AuthenticationPrincipal Jwt jwt,
                         @PathVariable UUID id) {
        return orderService.cancel(id, subjectOf(jwt));
    }

    private static String subjectOf(Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token has no subject");
        }
        return subject;
    }
}
