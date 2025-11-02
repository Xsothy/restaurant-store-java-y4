package com.restaurant.store.seeder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.store.entity.Category;
import com.restaurant.store.entity.Product;
import com.restaurant.store.repository.CategoryRepository;
import com.restaurant.store.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    private static final String API_URL = "https://ousa-food.vercel.app/api/products";

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting data seeding from external API...");

        // Check if data already exists
        if (categoryRepository.count() > 0 || productRepository.count() > 0) {
            log.info("Data already exists. Skipping seeding.");
            return;
        }

        try {
            // Fetch data from external API
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                seedCategories(root.get("categories"));
                seedProducts(root.get("products"));
                log.info("Data seeding completed successfully!");
            } else {
                log.error("Failed to fetch data from API. Status code: {}", response.statusCode());
            }

        } catch (Exception e) {
            log.error("Error during data seeding: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void seedCategories(JsonNode categoriesNode) {
        if (categoriesNode == null || !categoriesNode.isArray()) {
            log.warn("No categories found in API response");
            return;
        }

        Map<String, Category> categoryMap = new HashMap<>();

        for (JsonNode node : categoriesNode) {
            String nameEn = node.get("name_en").asText();
            String description = node.has("description") ? node.get("description").asText() : "";

            Category category = new Category(nameEn, description);
            category = categoryRepository.save(category);
            
            // Store mapping using the external UUID
            String externalId = node.get("id").asText();
            categoryMap.put(externalId, category);

            log.info("Created category: {} (ID: {})", nameEn, category.getId());
        }

        // Store the mapping for later use in products
        this.categoryMapping = categoryMap;
    }

    private Map<String, Category> categoryMapping = new HashMap<>();

    private void seedProducts(JsonNode productsNode) {
        if (productsNode == null || !productsNode.isArray()) {
            log.warn("No products found in API response");
            return;
        }

        for (JsonNode node : productsNode) {
            String nameEn = node.get("name_en").asText();
            String descriptionEn = node.has("description_en") ? node.get("description_en").asText() : "";
            BigDecimal price = BigDecimal.valueOf(node.get("price").asDouble());
            String imageUrl = node.has("image_url") ? node.get("image_url").asText() : null;
            Boolean isActive = node.has("active") ? node.get("active").asBoolean() : true;
            String categoryExternalId = node.get("category_id").asText();

            // Find the corresponding category
            Category category = categoryMapping.get(categoryExternalId);
            
            if (category == null) {
                log.warn("Category not found for product: {}. Skipping.", nameEn);
                continue;
            }

            Product product = new Product(nameEn, descriptionEn, price, imageUrl, isActive, category);
            productRepository.save(product);

            log.info("Created product: {} (Price: ${}, Category: {})", 
                    nameEn, price, category.getName());
        }

        log.info("Total products created: {}", productRepository.count());
        log.info("Total categories created: {}", categoryRepository.count());
    }
}
