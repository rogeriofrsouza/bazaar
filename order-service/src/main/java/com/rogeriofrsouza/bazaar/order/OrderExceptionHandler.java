package com.rogeriofrsouza.bazaar.order;

import com.rogeriofrsouza.bazaar.order.inventory.InventoryUnavailableException;
import com.rogeriofrsouza.bazaar.order.order.OrderNotFoundException;
import com.rogeriofrsouza.bazaar.order.order.OrderNotPendingException;
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

    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail handleNotFound(OrderNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(OrderNotPendingException.class)
    ProblemDetail handleConflict(OrderNotPendingException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }
}
