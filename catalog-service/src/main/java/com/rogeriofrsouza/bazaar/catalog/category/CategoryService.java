package com.rogeriofrsouza.bazaar.catalog.category;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        List<Category> categories = categoryRepository.findAll(Sort.by("name"));

        Map<UUID, List<Category>> childrenByParent = categories.stream()
                .filter(category -> category.getParentId() != null)
                .collect(Collectors.groupingBy(Category::getParentId));

        return categories.stream()
                .filter(category -> category.getParentId() == null)
                .map(category -> CategoryResponse.from(
                        category, childrenByParent.getOrDefault(category.getId(), List.of())))
                .toList();
    }
}
