package com.rogeriofrsouza.bazaar.catalog;

import com.rogeriofrsouza.bazaar.catalog.category.CategoryResponse;
import com.rogeriofrsouza.bazaar.catalog.category.CategoryService;
import com.rogeriofrsouza.bazaar.catalog.product.CreateProductRequest;
import com.rogeriofrsouza.bazaar.catalog.product.Product;
import com.rogeriofrsouza.bazaar.catalog.product.ProductRepository;
import com.rogeriofrsouza.bazaar.catalog.product.ProductResponse;
import com.rogeriofrsouza.bazaar.catalog.product.ProductService;
import com.rogeriofrsouza.bazaar.catalog.product.ProductStatus;
import io.hypersistence.tsid.TSID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(ContainersConfig.class)
@Transactional
class CatalogPersistenceTests {

    private static final PageRequest PAGE = PageRequest.of(0, 20, Sort.by("name"));

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void createsAndReadsProduct() {
        ProductResponse created = productService.create(request("Phone X", "phones"));
        assertThat(TSID.isValid(created.id())).isTrue();

        Long id = TSID.from(created.id()).toLong();
        ProductResponse found = productService.findById(id);
        assertThat(found).isEqualTo(created);
        assertThat(found.name()).isEqualTo("Phone X");
        assertThat(found.price()).isEqualByComparingTo("499.90");
        assertThat(found.currency()).isEqualTo("USD");

        Product product = productRepository.findByIdAndStatus(id, ProductStatus.ACTIVE).orElseThrow();
        assertThat(product.getCurrency()).isEqualTo(Currency.getInstance("USD"));
        assertThat(product.getCreatedAt()).isNotNull();
        assertThat(product.getUpdatedAt()).isNotNull();
    }

    @Test
    void filtersActiveProductsByCategoryAndIds() {
        ProductResponse phone = productService.create(request("Phone Y", "phones"));
        ProductResponse laptop = productService.create(request("Laptop Z", "laptops"));

        Page<ProductResponse> phones = productService.findAll("phones", null, PAGE);
        assertThat(phones.getContent()).extracting(ProductResponse::id).contains(phone.id())
                .doesNotContain(laptop.id());

        Page<ProductResponse> byIds = productService.findAll(null, List.of(TSID.from(laptop.id()).toLong()), PAGE);
        assertThat(byIds.getContent()).containsExactly(laptop);
        assertThat(byIds.getTotalElements()).isEqualTo(1);

        assertThat(productService.findAll("unknown", null, PAGE)).isEmpty();
    }

    @Test
    void listsCategoryTreeSortedByName() {
        List<CategoryResponse> roots = categoryService.list();

        assertThat(roots).hasSize(9).isSortedAccordingTo(Comparator.comparing(CategoryResponse::name));
        CategoryResponse electronics = roots.stream()
                .filter(root -> root.slug().equals("electronics"))
                .findFirst()
                .orElseThrow();
        assertThat(electronics.children()).isSortedAccordingTo(Comparator.comparing(CategoryResponse::name))
                .extracting(CategoryResponse::slug)
                .containsExactlyInAnyOrder("cameras", "headphones", "phones", "smartwatches", "tablets", "tvs");
        assertThat(electronics.children()).allSatisfy(child -> assertThat(child.children()).isNull());
        assertThat(roots).allSatisfy(root -> {
            assertThat(TSID.isValid(root.id())).isTrue();
            assertThat(root.children()).allSatisfy(child -> assertThat(TSID.isValid(child.id())).isTrue());
        });
    }

    private static CreateProductRequest request(String name, String category) {
        return new CreateProductRequest(name, null, new BigDecimal("499.90"), Currency.getInstance("USD"), null,
                category);
    }
}
