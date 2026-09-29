package com.rogeriofrsouza.bazaar.inventory;

import com.rogeriofrsouza.bazaar.inventory.item.InsufficientStockException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class InventoryExceptionHandler {

    @ExceptionHandler(InsufficientStockException.class)
    ProblemDetail handleConflict(InsufficientStockException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }
}
