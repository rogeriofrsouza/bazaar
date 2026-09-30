package com.rogeriofrsouza.bazaar.catalog.product;

import com.rogeriofrsouza.bazaar.catalog.category.Category;
import com.rogeriofrsouza.bazaar.catalog.category.CategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;

import static com.rogeriofrsouza.bazaar.catalog.product.ProductSpecifications.codeIn;
import static com.rogeriofrsouza.bazaar.catalog.product.ProductSpecifications.hasStatus;
import static com.rogeriofrsouza.bazaar.catalog.product.ProductSpecifications.inCategory;

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
    public Page<ProductResponse> findActive(String category, Collection<String> codes, Pageable pageable) {
        List<String> upperCodes = codes == null ? null : codes.stream().map(String::toUpperCase).toList();
        Specification<Product> specification = hasStatus(ProductStatus.ACTIVE)
                .and(inCategory(category))
                .and(codeIn(upperCodes));

        return productRepository.findAll(specification, pageable)
                .map(ProductResponse::from);
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
