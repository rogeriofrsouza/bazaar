package com.rogeriofrsouza.bazaar.catalog.product;

import io.hypersistence.tsid.TSID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RequestMapping("/api/products")
@RestController
class ProductController {

    private final ProductService productService;

    ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    PagedModel<ProductResponse> list(@RequestParam(required = false) @Nullable String category,
                                     @RequestParam(required = false) @Size(max = 20) @Nullable List<TSID> ids,
                                     @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        List<Long> productIds = ids == null ? null : ids.stream().map(TSID::toLong).toList();
        Page<ProductResponse> page = productService.findAll(category, productIds, pageable);
        return new PagedModel<>(page);
    }

    @GetMapping("/{id}")
    ProductResponse get(@PathVariable TSID id) {
        return productService.findById(id.toLong());
    }

    @PostMapping
    ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse product = productService.create(request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(product.id())
                .toUri();

        return ResponseEntity.created(location).body(product);
    }
}
