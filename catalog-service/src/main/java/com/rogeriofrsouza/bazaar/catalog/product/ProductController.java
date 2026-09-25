package com.rogeriofrsouza.bazaar.catalog.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RequestMapping("/api/products")
@RestController
class ProductController {

    private final ProductRepository productRepository;

    ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
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
}
