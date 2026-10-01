package com.rogeriofrsouza.bazaar.catalog.category;

import org.springframework.boot.CommandLineRunner;
import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
class CategorySeeder implements CommandLineRunner {

    private static final List<SeedCategory> SEED_CATEGORIES = List.of(
            category("electronics", "Electronics",
                    category("phones", "Phones"),
                    category("tablets", "Tablets"),
                    category("headphones", "Headphones"),
                    category("cameras", "Cameras"),
                    category("tvs", "TVs"),
                    category("smartwatches", "Smartwatches")),
            category("computers", "Computers",
                    category("laptops", "Laptops"),
                    category("desktops", "Desktops"),
                    category("monitors", "Monitors"),
                    category("keyboards-mice", "Keyboards & Mice"),
                    category("storage", "Storage")),
            category("home-kitchen", "Home & Kitchen",
                    category("furniture", "Furniture"),
                    category("kitchen-appliances", "Kitchen Appliances"),
                    category("cookware", "Cookware"),
                    category("bedding", "Bedding"),
                    category("home-decor", "Home Decor")),
            category("fashion", "Fashion",
                    category("mens-clothing", "Men's Clothing"),
                    category("womens-clothing", "Women's Clothing"),
                    category("shoes", "Shoes"),
                    category("bags", "Bags"),
                    category("jewelry", "Jewelry")),
            category("books", "Books",
                    category("fiction", "Fiction"),
                    category("non-fiction", "Non-Fiction"),
                    category("technology-books", "Technology"),
                    category("childrens-books", "Children's Books")),
            category("sports-outdoors", "Sports & Outdoors",
                    category("fitness", "Fitness"),
                    category("cycling", "Cycling"),
                    category("camping", "Camping"),
                    category("team-sports", "Team Sports")),
            category("toys-games", "Toys & Games",
                    category("board-games", "Board Games"),
                    category("video-games", "Video Games"),
                    category("puzzles", "Puzzles"),
                    category("building-toys", "Building Toys")),
            category("beauty", "Beauty & Personal Care",
                    category("skincare", "Skincare"),
                    category("makeup", "Makeup"),
                    category("hair-care", "Hair Care"),
                    category("fragrances", "Fragrances")),
            category("groceries", "Groceries",
                    category("beverages", "Beverages"),
                    category("snacks", "Snacks"),
                    category("pantry", "Pantry"))
    );

    private final CategoryRepository categoryRepository;
    private final JdbcAggregateOperations jdbcAggregateOperations;

    CategorySeeder(CategoryRepository categoryRepository, JdbcAggregateOperations jdbcAggregateOperations) {
        this.categoryRepository = categoryRepository;
        this.jdbcAggregateOperations = jdbcAggregateOperations;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (categoryRepository.count() > 0) {
            return;
        }

        List<Category> parents = new ArrayList<>();
        List<Category> children = new ArrayList<>();

        for (SeedCategory seedCategory : SEED_CATEGORIES) {
            Category parentCategory = Category.create(seedCategory.slug(), seedCategory.name(), null);
            parents.add(parentCategory);

            seedCategory.children()
                    .forEach(child -> children.add(Category.create(child.slug(), child.name(), parentCategory.getId())));
        }

        jdbcAggregateOperations.insertAll(parents);
        jdbcAggregateOperations.insertAll(children);
    }

    private static SeedCategory category(String slug, String name, SeedCategory... children) {
        return new SeedCategory(slug, name, List.of(children));
    }

    private record SeedCategory(String slug, String name, List<SeedCategory> children) {
    }
}
