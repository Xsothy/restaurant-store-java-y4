package com.restaurant.store.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.store.dto.request.CreateOrderRequest;
import com.restaurant.store.dto.request.OrderItemRequest;
import com.restaurant.store.entity.Category;
import com.restaurant.store.entity.Customer;
import com.restaurant.store.entity.OrderType;
import com.restaurant.store.entity.Product;
import com.restaurant.store.repository.CartItemRepository;
import com.restaurant.store.repository.CartRepository;
import com.restaurant.store.repository.CategoryRepository;
import com.restaurant.store.repository.CustomerRepository;
import com.restaurant.store.repository.OrderItemRepository;
import com.restaurant.store.repository.OrderRepository;
import com.restaurant.store.repository.ProductRepository;
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
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Pickup Controller Integration Tests")
class PickupControllerIntegrationTest {

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
    private Product pickupProduct;

    @BeforeEach
    void setUp() throws Exception {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        customerRepository.deleteAll();

        customer = new Customer();
        customer.setEmail("pickup@example.com");
        customer.setPasswordHash(passwordEncoder.encode("password123"));
        customer.setName("Pickup User");
        customer.setPhone("+1234567890");
        customer.setAddress("123 Pickup St");
        customer = customerRepository.save(customer);

        Category category = new Category();
        category.setName("Pickup Category");
        category.setDescription("Test");
        category = categoryRepository.save(category);

        pickupProduct = new Product();
        pickupProduct.setName("Pickup Product");
        pickupProduct.setDescription("Pickup ready item");
        pickupProduct.setPrice(BigDecimal.valueOf(12.50));
        pickupProduct.setCategory(category);
        pickupProduct.setIsAvailable(true);
        pickupProduct = productRepository.save(pickupProduct);

        String loginPayload = "{\"email\":\"pickup@example.com\",\"password\":\"password123\"}";
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        authToken = "Bearer " + objectMapper.readTree(loginResponse).get("data").get("token").asText();
    }

    @Test
    @DisplayName("Should retrieve pickup details for pickup order")
    void testGetPickupDetails() throws Exception {
        CreateOrderRequest orderRequest = new CreateOrderRequest();
        orderRequest.setOrderType(OrderType.PICKUP);
        orderRequest.setPhoneNumber("+1234567890");
        orderRequest.setDeliveryAddress("Storefront");
        orderRequest.setOrderItems(List.of(new OrderItemRequest(pickupProduct.getId(), 1, null)));

        String orderResponse = mockMvc.perform(post("/api/orders")
                        .header("Authorization", authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long orderId = objectMapper.readTree(orderResponse).get("data").get("id").asLong();

        mockMvc.perform(get("/api/pickups/{orderId}", orderId)
                        .header("Authorization", authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(orderId))
                .andExpect(jsonPath("$.data.pickupCode").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("AWAITING_CONFIRMATION"));
    }
}
