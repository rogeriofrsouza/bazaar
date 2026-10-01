package com.rogeriofrsouza.bazaar.catalog.product;

import com.rogeriofrsouza.bazaar.catalog.category.Category;
import com.rogeriofrsouza.bazaar.catalog.category.CategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.rogeriofrsouza.bazaar.catalog.product.ProductCriteria.*;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductCodeGenerator productCodeGenerator;
    private final JdbcAggregateOperations jdbcAggregateOperations;

    ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
                   ProductCodeGenerator productCodeGenerator, JdbcAggregateOperations jdbcAggregateOperations) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productCodeGenerator = productCodeGenerator;
        this.jdbcAggregateOperations = jdbcAggregateOperations;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> findActive(String category, Collection<String> codes, Pageable pageable) {
        Long categoryId = null;
        if (category != null) {
            Optional<Category> found = categoryRepository.findBySlug(category);
            if (found.isEmpty()) {
                return Page.empty(pageable);
            }
            categoryId = found.get().getId();
        }

        List<String> upperCodes = codes == null ? null : codes.stream().map(String::toUpperCase).toList();
        Criteria criteria = Criteria.from(Stream.of(
                        hasStatus(ProductStatus.ACTIVE),
                        inCategory(categoryId),
                        codeIn(upperCodes))
                .filter(criterion -> !criterion.isEmpty())
                .toList());

        Query query = Query.query(criteria);
        List<Product> products = jdbcAggregateOperations.findAll(query.with(pageable), Product.class);

        return PageableExecutionUtils.getPage(
                        products, pageable, () -> jdbcAggregateOperations.count(query, Product.class))
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
                category.getId()
        );
        return ProductResponse.from(productRepository.save(product));
    }
}
