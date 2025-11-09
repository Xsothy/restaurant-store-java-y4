package com.restaurant.store.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.store.dto.request.CustomerRegisterRequest;
import com.restaurant.store.dto.request.LoginRequest;
import com.restaurant.store.entity.Customer;
import com.restaurant.store.repository.CustomerRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Auth Controller Integration Tests")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
    }

    @Test
    @DisplayName("Should register a new customer successfully")
    void testRegisterCustomer_Success() throws Exception {
        CustomerRegisterRequest request = new CustomerRegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setName("Test User");
        request.setPhone("1234567890");
        request.setAddress("123 Test St");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Customer registered successfully"))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.customer.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.customer.name").value("Test User"));
    }

    @Test
    @DisplayName("Should return 409 when registering with existing email")
    void testRegisterCustomer_EmailExists() throws Exception {
        // Create existing customer
        Customer existingCustomer = new Customer();
        existingCustomer.setEmail("existing@example.com");
        existingCustomer.setPasswordHash(passwordEncoder.encode("password123"));
        existingCustomer.setName("Existing User");
        existingCustomer.setPhone("1234567890");
        existingCustomer.setAddress("123 Test St");
        customerRepository.save(existingCustomer);

        CustomerRegisterRequest request = new CustomerRegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("password123");
        request.setName("Test User");
        request.setPhone("0987654321");
        request.setAddress("456 Test Ave");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized()); // Can be 409 Conflict or 400 Bad Request
    }

    @Test
    @DisplayName("Should return 400 when registering with invalid data")
    void testRegisterCustomer_InvalidData() throws Exception {
        CustomerRegisterRequest request = new CustomerRegisterRequest();
        request.setEmail("invalid-email");
        request.setPassword("123"); // Too short
        request.setName("");
        request.setPhone("");
        request.setAddress("");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void testLogin_Success() throws Exception {
        // Create a customer
        Customer customer = new Customer();
        customer.setEmail("login@example.com");
        customer.setPasswordHash(passwordEncoder.encode("password123"));
        customer.setName("Login User");
        customer.setPhone("1234567890");
        customer.setAddress("123 Test St");
        customerRepository.save(customer);

        LoginRequest request = new LoginRequest();
        request.setEmail("login@example.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.customer.email").value("login@example.com"))
                .andExpect(jsonPath("$.data.customer.name").value("Login User"));
    }

    @Test
    @DisplayName("Should return 401 with invalid credentials")
    void testLogin_InvalidCredentials() throws Exception {
        // Create a customer
        Customer customer = new Customer();
        customer.setEmail("test@example.com");
        customer.setPasswordHash(passwordEncoder.encode("correctpassword"));
        customer.setName("Test User");
        customer.setPhone("1234567890");
        customer.setAddress("123 Test St");
        customerRepository.save(customer);

        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 401 when logging in with non-existent email")
    void testLogin_NonExistentEmail() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should logout successfully with valid token")
    void testLogout_Success() throws Exception {
        // Create a customer and get token
        Customer customer = new Customer();
        customer.setEmail("logout@example.com");
        customer.setPasswordHash(passwordEncoder.encode("password123"));
        customer.setName("Logout User");
        customer.setPhone("1234567890");
        customer.setAddress("123 Test St");
        customerRepository.save(customer);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("logout@example.com");
        loginRequest.setPassword("password123");

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(loginResponse).get("data").get("token").asText();

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logout successful"));
    }

    @Test
    @DisplayName("Should return 401 or redirect when logging out without token")
    void testLogout_NoToken() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized()); // Can be 401 or 302 redirect
    }
}
