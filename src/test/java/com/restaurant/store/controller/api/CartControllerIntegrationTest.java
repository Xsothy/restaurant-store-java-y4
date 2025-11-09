package com.restaurant.store.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.store.dto.request.AddToCartRequest;
import com.restaurant.store.dto.request.LoginRequest;
import com.restaurant.store.dto.request.UpdateCartItemRequest;
import com.restaurant.store.entity.*;
import com.restaurant.store.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Cart Controller Integration Tests")
class CartControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String authToken;
    private Customer customer;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() throws Exception {
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        customerRepository.deleteAll();

        // Create customer
        customer = new Customer();
        customer.setEmail("cart@example.com");
        customer.setPasswordHash(passwordEncoder.encode("password123"));
        customer.setName("Cart User");
        customer.setPhone("1234567890");
        customer.setAddress("123 Test St");
        customer = customerRepository.save(customer);

        // Create category
        Category category = new Category();
        category.setName("Test Category");
        category.setDescription("Test Description");
        category = categoryRepository.save(category);

        // Create products
        product1 = new Product();
        product1.setName("Product 1");
        product1.setDescription("Description 1");
        product1.setPrice(BigDecimal.valueOf(10.00));
        product1.setCategory(category);
        product1.setIsAvailable(true);
        product1 = productRepository.save(product1);

        product2 = new Product();
        product2.setName("Product 2");
        product2.setDescription("Description 2");
        product2.setPrice(BigDecimal.valueOf(15.00));
        product2.setCategory(category);
        product2.setIsAvailable(true);
        product2 = productRepository.save(product2);

        // Login to get token
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("cart@example.com");
        loginRequest.setPassword("password123");

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        authToken = "Bearer " + objectMapper.readTree(loginResponse).get("data").get("token").asText();
    }

    @Test
    @DisplayName("Should get empty cart for new user")
    void testGetCart_Empty() throws Exception {
        mockMvc.perform(get("/api/cart")
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(0)))
                .andExpect(jsonPath("$.data.total").value(0.0));
    }

    @Test
    @DisplayName("Should add item to cart successfully")
    void testAddToCart_Success() throws Exception {
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(product1.getId());
        request.setQuantity(2);

        mockMvc.perform(post("/api/cart/add")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].productName").value("Product 1"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.items[0].price").value(10.00))
                .andExpect(jsonPath("$.data.total").value(20.00));
    }

    @Test
    @DisplayName("Should add multiple items to cart")
    void testAddToCart_MultipleItems() throws Exception {
        // Add first item
        AddToCartRequest request1 = new AddToCartRequest();
        request1.setProductId(product1.getId());
        request1.setQuantity(2);

        mockMvc.perform(post("/api/cart/add")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        // Add second item
        AddToCartRequest request2 = new AddToCartRequest();
        request2.setProductId(product2.getId());
        request2.setQuantity(1);

        mockMvc.perform(post("/api/cart/add")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.total").value(35.00)); // 2*10 + 1*15
    }

    @Test
    @DisplayName("Should return 404 when adding non-existent product")
    void testAddToCart_ProductNotFound() throws Exception {
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(99999L);
        request.setQuantity(1);

        mockMvc.perform(post("/api/cart/add")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 401 when adding to cart without token")
    void testAddToCart_Unauthorized() throws Exception {
        AddToCartRequest request = new AddToCartRequest();
        request.setProductId(product1.getId());
        request.setQuantity(1);

        mockMvc.perform(post("/api/cart/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should update cart item quantity successfully")
    void testUpdateCartItem_Success() throws Exception {
        // Add item to cart first
        AddToCartRequest addRequest = new AddToCartRequest();
        addRequest.setProductId(product1.getId());
        addRequest.setQuantity(2);

        String addResponse = mockMvc.perform(post("/api/cart/add")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long cartItemId = objectMapper.readTree(addResponse)
                .get("data").get("items").get(0).get("id").asLong();

        // Update quantity
        UpdateCartItemRequest updateRequest = new UpdateCartItemRequest();
        updateRequest.setQuantity(5);

        mockMvc.perform(put("/api/cart/items/{cartItemId}", cartItemId)
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].quantity").value(5))
                .andExpect(jsonPath("$.data.total").value(50.00)); // 5*10
    }

    @Test
    @DisplayName("Should remove item from cart successfully")
    void testRemoveFromCart_Success() throws Exception {
        // Add item to cart first
        AddToCartRequest addRequest = new AddToCartRequest();
        addRequest.setProductId(product1.getId());
        addRequest.setQuantity(2);

        String addResponse = mockMvc.perform(post("/api/cart/add")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long cartItemId = objectMapper.readTree(addResponse)
                .get("data").get("items").get(0).get("id").asLong();

        // Remove item
        mockMvc.perform(delete("/api/cart/items/{cartItemId}", cartItemId)
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items", hasSize(0)))
                .andExpect(jsonPath("$.data.total").value(0.0));
    }

    @Test
    @DisplayName("Should clear cart successfully")
    void testClearCart_Success() throws Exception {
        // Add multiple items
        AddToCartRequest request1 = new AddToCartRequest();
        request1.setProductId(product1.getId());
        request1.setQuantity(2);
        mockMvc.perform(post("/api/cart/add")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        AddToCartRequest request2 = new AddToCartRequest();
        request2.setProductId(product2.getId());
        request2.setQuantity(1);
        mockMvc.perform(post("/api/cart/add")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk());

        // Clear cart
        mockMvc.perform(delete("/api/cart/clear")
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify cart is empty
        mockMvc.perform(get("/api/cart")
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(0)))
                .andExpect(jsonPath("$.data.total").value(0.0));
    }

    @Test
    @DisplayName("Should return 404 when updating non-existent cart item")
    void testUpdateCartItem_NotFound() throws Exception {
        UpdateCartItemRequest updateRequest = new UpdateCartItemRequest();
        updateRequest.setQuantity(5);

        mockMvc.perform(put("/api/cart/items/{cartItemId}", 99999L)
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 when removing non-existent cart item")
    void testRemoveFromCart_NotFound() throws Exception {
        mockMvc.perform(delete("/api/cart/items/{cartItemId}", 99999L)
                        .header("Authorization", authToken))
                .andExpect(status().isNotFound());
    }
}
