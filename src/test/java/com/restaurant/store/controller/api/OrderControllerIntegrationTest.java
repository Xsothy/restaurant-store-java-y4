package com.restaurant.store.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.store.dto.request.AddToCartRequest;
import com.restaurant.store.dto.request.CreateOrderRequest;
import com.restaurant.store.dto.request.LoginRequest;
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
@DisplayName("Order Controller Integration Tests")
class OrderControllerIntegrationTest {

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
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String authToken;
    private Customer customer;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() throws Exception {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        customerRepository.deleteAll();

        // Create customer
        customer = new Customer();
        customer.setEmail("order@example.com");
        customer.setPasswordHash(passwordEncoder.encode("password123"));
        customer.setName("Order User");
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
        loginRequest.setEmail("order@example.com");
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
    @DisplayName("Should create order from cart successfully")
    void testCreateOrder_Success() throws Exception {
        // Add items to cart
        AddToCartRequest cartRequest = new AddToCartRequest();
        cartRequest.setProductId(product1.getId());
        cartRequest.setQuantity(2);
        mockMvc.perform(post("/api/cart/add")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartRequest)))
                .andExpect(status().isOk());

        // Create order
        CreateOrderRequest orderRequest = new CreateOrderRequest();
        orderRequest.setDeliveryAddress("123 Delivery St");
        orderRequest.setPhoneNumber("1234567890");
        orderRequest.setOrderType(OrderType.DELIVERY);

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Order created successfully"))
                .andExpect(jsonPath("$.data.total").value(20.00))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.orderType").value("DELIVERY"))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].productName").value("Product 1"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2));
    }

    @Test
    @DisplayName("Should return 400 when creating order with empty cart")
    void testCreateOrder_EmptyCart() throws Exception {
        CreateOrderRequest orderRequest = new CreateOrderRequest();
        orderRequest.setDeliveryAddress("123 Delivery St");
        orderRequest.setPhoneNumber("1234567890");
        orderRequest.setOrderType(OrderType.DELIVERY);

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 401 when creating order without authentication")
    void testCreateOrder_Unauthorized() throws Exception {
        CreateOrderRequest orderRequest = new CreateOrderRequest();
        orderRequest.setDeliveryAddress("123 Delivery St");
        orderRequest.setPhoneNumber("1234567890");
        orderRequest.setOrderType(OrderType.DELIVERY);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should get order by ID successfully")
    void testGetOrderById_Success() throws Exception {
        // Create order first
        AddToCartRequest cartRequest = new AddToCartRequest();
        cartRequest.setProductId(product1.getId());
        cartRequest.setQuantity(1);
        mockMvc.perform(post("/api/cart/add")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartRequest)))
                .andExpect(status().isOk());

        CreateOrderRequest orderRequest = new CreateOrderRequest();
        orderRequest.setDeliveryAddress("123 Delivery St");
        orderRequest.setPhoneNumber("1234567890");
        orderRequest.setOrderType(OrderType.DELIVERY);

        String orderResponse = mockMvc.perform(post("/api/orders")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long orderId = objectMapper.readTree(orderResponse).get("data").get("id").asLong();

        // Get order by ID
        mockMvc.perform(get("/api/orders/{orderId}", orderId)
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(orderId))
                .andExpect(jsonPath("$.data.total").value(10.00))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("Should return 404 when getting non-existent order")
    void testGetOrderById_NotFound() throws Exception {
        mockMvc.perform(get("/api/orders/{orderId}", 99999L)
                        .header("Authorization", authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should get order status successfully")
    void testGetOrderStatus_Success() throws Exception {
        // Create order first
        AddToCartRequest cartRequest = new AddToCartRequest();
        cartRequest.setProductId(product1.getId());
        cartRequest.setQuantity(1);
        mockMvc.perform(post("/api/cart/add")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartRequest)))
                .andExpect(status().isOk());

        CreateOrderRequest orderRequest = new CreateOrderRequest();
        orderRequest.setDeliveryAddress("123 Delivery St");
        orderRequest.setPhoneNumber("1234567890");
        orderRequest.setOrderType(OrderType.DELIVERY);

        String orderResponse = mockMvc.perform(post("/api/orders")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long orderId = objectMapper.readTree(orderResponse).get("data").get("id").asLong();

        // Get order status
        mockMvc.perform(get("/api/orders/{orderId}/status", orderId)
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("PENDING"));
    }

    @Test
    @DisplayName("Should get my orders successfully")
    void testGetMyOrders_Success() throws Exception {
        // Create first order
        AddToCartRequest cartRequest1 = new AddToCartRequest();
        cartRequest1.setProductId(product1.getId());
        cartRequest1.setQuantity(1);
        mockMvc.perform(post("/api/cart/add")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartRequest1)))
                .andExpect(status().isOk());

        CreateOrderRequest orderRequest1 = new CreateOrderRequest();
        orderRequest1.setDeliveryAddress("123 Delivery St");
        orderRequest1.setPhoneNumber("1234567890");
        orderRequest1.setOrderType(OrderType.DELIVERY);
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest1)))
                .andExpect(status().isCreated());

        // Create second order
        AddToCartRequest cartRequest2 = new AddToCartRequest();
        cartRequest2.setProductId(product2.getId());
        cartRequest2.setQuantity(2);
        mockMvc.perform(post("/api/cart/add")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartRequest2)))
                .andExpect(status().isOk());

        CreateOrderRequest orderRequest2 = new CreateOrderRequest();
        orderRequest2.setDeliveryAddress("456 Delivery Ave");
        orderRequest2.setPhoneNumber("1234567890");
        orderRequest2.setOrderType(OrderType.PICKUP);
        mockMvc.perform(post("/api/orders")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest2)))
                .andExpect(status().isCreated());

        // Get my orders
        mockMvc.perform(get("/api/orders/my-orders")
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[*].status", everyItem(is("PENDING"))));
    }

    @Test
    @DisplayName("Should get empty list for customer with no orders")
    void testGetMyOrders_EmptyList() throws Exception {
        mockMvc.perform(get("/api/orders/my-orders")
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @DisplayName("Should cancel order successfully")
    void testCancelOrder_Success() throws Exception {
        // Create order first
        AddToCartRequest cartRequest = new AddToCartRequest();
        cartRequest.setProductId(product1.getId());
        cartRequest.setQuantity(1);
        mockMvc.perform(post("/api/cart/add")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartRequest)))
                .andExpect(status().isOk());

        CreateOrderRequest orderRequest = new CreateOrderRequest();
        orderRequest.setDeliveryAddress("123 Delivery St");
        orderRequest.setPhoneNumber("1234567890");
        orderRequest.setOrderType(OrderType.DELIVERY);

        String orderResponse = mockMvc.perform(post("/api/orders")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long orderId = objectMapper.readTree(orderResponse).get("data").get("id").asLong();

        // Cancel order
        mockMvc.perform(put("/api/orders/{orderId}/cancel", orderId)
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify order is cancelled
        mockMvc.perform(get("/api/orders/{orderId}", orderId)
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("Should return 404 when cancelling non-existent order")
    void testCancelOrder_NotFound() throws Exception {
        mockMvc.perform(put("/api/orders/{orderId}/cancel", 99999L)
                        .header("Authorization", authToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should create payment intent successfully")
    void testCreatePaymentIntent_Success() throws Exception {
        // Create order first
        AddToCartRequest cartRequest = new AddToCartRequest();
        cartRequest.setProductId(product1.getId());
        cartRequest.setQuantity(1);
        mockMvc.perform(post("/api/cart/add")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartRequest)))
                .andExpect(status().isOk());

        CreateOrderRequest orderRequest = new CreateOrderRequest();
        orderRequest.setDeliveryAddress("123 Delivery St");
        orderRequest.setPhoneNumber("1234567890");
        orderRequest.setOrderType(OrderType.DELIVERY);

        String orderResponse = mockMvc.perform(post("/api/orders")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long orderId = objectMapper.readTree(orderResponse).get("data").get("id").asLong();

        // Note: This might fail in test environment without valid Stripe credentials
        // We're testing the endpoint is accessible, not Stripe integration
        mockMvc.perform(post("/api/orders/{orderId}/payment-intent", orderId)
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
