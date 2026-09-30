package com.rogeriofrsouza.bazaar.catalog.product;

import com.rogeriofrsouza.bazaar.catalog.category.Category;
import com.rogeriofrsouza.bazaar.catalog.category.CategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductCodeGenerator productCodeGenerator;

    ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
                   ProductCodeGenerator productCodeGenerator) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productCodeGenerator = productCodeGenerator;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> findActive(String category, Pageable pageable) {
        Page<Product> products = category == null
                ? productRepository.findByStatus(ProductStatus.ACTIVE, pageable)
                : productRepository.findByStatusAndCategorySlug(ProductStatus.ACTIVE, category, pageable);

        return products.map(ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public ProductResponse findActiveByCode(String code) {
        return productRepository.findByCodeAndStatus(code.toUpperCase(), ProductStatus.ACTIVE)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product " + code + " not found"));
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        Category category = categoryRepository.findBySlug(request.category())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Category " + request.category() + " not found"));

        Product product = Product.create(
                productCodeGenerator.generate(),
                request.name(),
                request.description(),
                request.price(),
                request.currency(),
                request.imageUrl(),
                category
        );
        return ProductResponse.from(productRepository.save(product));
    }
}
