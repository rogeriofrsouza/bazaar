package com.rogeriofrsouza.bazaar.catalog.product;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;

@RequestMapping("/api/products")
@RestController
class ProductController {

    private final ProductRepository productRepository;
    private final ProductService productService;

    ProductController(ProductRepository productRepository, ProductService productService) {
        this.productRepository = productRepository;
        this.productService = productService;
    }

    @GetMapping
    PagedModel<ProductResponse> list(
        @RequestParam(required = false) String category,
        @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        Page<Product> products = category == null
            ? productRepository.findByStatus(ProductStatus.ACTIVE, pageable)
            : productRepository.findByStatusAndCategorySlug(ProductStatus.ACTIVE, category, pageable);
        return new PagedModel<>(products.map(ProductResponse::from));
    }

    @GetMapping("/{code}")
    ProductResponse get(@PathVariable ProductCode code) {
        return productRepository.findByCodeAndStatus(code, ProductStatus.ACTIVE)
            .map(ProductResponse::from)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product " + code.value() + " not found"));
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
