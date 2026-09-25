package com.rogeriofrsouza.bazaar.catalog.product;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RequestMapping("/api/products")
@RestController
class ProductController {

    private final ProductService productService;

    ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    PagedModel<ProductResponse> list(@RequestParam(required = false) String category,
                                     @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        Page<ProductResponse> page = productService.findActive(category, pageable);
        return new PagedModel<>(page);
    }

    @GetMapping("/{code}")
    ProductResponse get(@PathVariable ProductCode code) {
        return productService.findActiveByCode(code);
    }

    @PostMapping
    ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse product = productService.create(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{code}")
            .buildAndExpand(product.code())
            .toUri();

        return ResponseEntity.created(location).body(product);
    }
}
