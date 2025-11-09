package com.restaurant.store.controller.api;

import com.restaurant.store.entity.Category;
import com.restaurant.store.entity.Product;
import com.restaurant.store.repository.CategoryRepository;
import com.restaurant.store.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Product Controller Integration Tests")
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    private Category category1;
    private Category category2;
    private Product product1;
    private Product product2;
    private Product product3;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        // Create categories
        category1 = new Category();
        category1.setName("Burgers");
        category1.setDescription("Delicious burgers");
        category1 = categoryRepository.save(category1);

        category2 = new Category();
        category2.setName("Pizzas");
        category2.setDescription("Italian pizzas");
        category2 = categoryRepository.save(category2);

        // Create products
        product1 = new Product();
        product1.setName("Classic Burger");
        product1.setDescription("A classic burger");
        product1.setPrice(BigDecimal.valueOf(9.99));
        product1.setCategory(category1);
        product1.setIsAvailable(true);
        product1.setImageUrl("burger.jpg");
        product1 = productRepository.save(product1);

        product2 = new Product();
        product2.setName("Cheese Burger");
        product2.setDescription("Burger with cheese");
        product2.setPrice(BigDecimal.valueOf(11.99));
        product2.setCategory(category1);
        product2.setIsAvailable(true);
        product2.setImageUrl("cheeseburger.jpg");
        product2 = productRepository.save(product2);

        product3 = new Product();
        product3.setName("Margherita Pizza");
        product3.setDescription("Classic Italian pizza");
        product3.setPrice(BigDecimal.valueOf(12.99));
        product3.setCategory(category2);
        product3.setIsAvailable(false); // Not available
        product3.setImageUrl("pizza.jpg");
        product3 = productRepository.save(product3);
    }

    @Test
    @DisplayName("Should get all categories successfully")
    void testGetAllCategories_Success() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("Burgers", "Pizzas")));
    }

    @Test
    @DisplayName("Should get all products successfully")
    void testGetAllProducts_Success() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[*].name", 
                    containsInAnyOrder("Classic Burger", "Cheese Burger", "Margherita Pizza")));
    }

    @Test
    @DisplayName("Should get only available products")
    void testGetAllProducts_AvailableOnly() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("availableOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].isAvailable", everyItem(is(true))))
                .andExpect(jsonPath("$.data[*].name", 
                    containsInAnyOrder("Classic Burger", "Cheese Burger")));
    }

    @Test
    @DisplayName("Should get products by category")
    void testGetAllProducts_ByCategory() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("categoryId", category1.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].name", 
                    containsInAnyOrder("Classic Burger", "Cheese Burger")));
    }

    @Test
    @DisplayName("Should get available products by category")
    void testGetAllProducts_ByCategoryAvailableOnly() throws Exception {
        mockMvc.perform(get("/api/products")
                        .param("categoryId", category1.getId().toString())
                        .param("availableOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].isAvailable", everyItem(is(true))));
    }

    @Test
    @DisplayName("Should get product by ID successfully")
    void testGetProductById_Success() throws Exception {
        mockMvc.perform(get("/api/products/{productId}", product1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(product1.getId()))
                .andExpect(jsonPath("$.data.name").value("Classic Burger"))
                .andExpect(jsonPath("$.data.price").value(9.99))
                .andExpect(jsonPath("$.data.isAvailable").value(true))
                .andExpect(jsonPath("$.data.category.name").value("Burgers"));
    }

    @Test
    @DisplayName("Should return 404 when product not found")
    void testGetProductById_NotFound() throws Exception {
        mockMvc.perform(get("/api/products/{productId}", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should get products by category endpoint")
    void testGetProductsByCategory_Success() throws Exception {
        mockMvc.perform(get("/api/categories/{categoryId}/products", category1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].name", 
                    containsInAnyOrder("Classic Burger", "Cheese Burger")));
    }

    @Test
    @DisplayName("Should return 404 when category not found")
    void testGetProductsByCategory_CategoryNotFound() throws Exception {
        mockMvc.perform(get("/api/categories/{categoryId}/products", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return empty list for category with no products")
    void testGetProductsByCategory_EmptyList() throws Exception {
        Category emptyCategory = new Category();
        emptyCategory.setName("Empty Category");
        emptyCategory.setDescription("No products");
        emptyCategory = categoryRepository.save(emptyCategory);

        mockMvc.perform(get("/api/categories/{categoryId}/products", emptyCategory.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }
}
