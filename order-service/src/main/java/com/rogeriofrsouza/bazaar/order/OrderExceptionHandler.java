package com.rogeriofrsouza.bazaar.order;

import com.rogeriofrsouza.bazaar.order.inventory.InventoryUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class OrderExceptionHandler {

    @ExceptionHandler(InventoryUnavailableException.class)
    ProblemDetail handleBadGateway(InventoryUnavailableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }
}
