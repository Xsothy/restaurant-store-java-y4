package com.restaurant.store.dto.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.restaurant.store.dto.admin.request.*;
import com.restaurant.store.dto.admin.response.*;
import com.restaurant.store.dto.admin.websocket.WebSocketMessageDTO;
import com.restaurant.store.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple unit tests for Admin DTOs without Spring Boot context
 */
class AdminDtoSimpleTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void testCategoryDtoSerialization() throws Exception {
        CategoryDTO dto = CategoryDTO.builder()
                .id(1L)
                .name("Test Category")
                .description("Test Description")
                .createdAt(LocalDateTime.now())
                .build();

        String json = objectMapper.writeValueAsString(dto);
        CategoryDTO deserialized = objectMapper.readValue(json, CategoryDTO.class);

        assertEquals(dto.getId(), deserialized.getId());
        assertEquals(dto.getName(), deserialized.getName());
        assertNotNull(json);
    }

    @Test
    void testDeliveryDtoWithDeliveryStatus() throws Exception {
        DeliveryDTO dto = DeliveryDTO.builder()
                .id(1L)
                .orderId(100L)
                .status(DeliveryStatus.DELIVERED)
                .deliveryAddress("123 Test St")
                .build();

        String json = objectMapper.writeValueAsString(dto);
        DeliveryDTO deserialized = objectMapper.readValue(json, DeliveryDTO.class);

        assertEquals(DeliveryStatus.DELIVERED, deserialized.getStatus());
        assertEquals("123 Test St", deserialized.getDeliveryAddress());
    }

    @Test
    void testOrderDtoWithEnums() throws Exception {
        OrderDTO dto = OrderDTO.builder()
                .id(1L)
                .customerName("John Doe")
                .customerPhone("555-1234")
                .customerDetails("Test customer")
                .status(OrderStatus.CONFIRMED)
                .orderType(OrderType.DELIVERY)
                .totalPrice(new BigDecimal("25.50"))
                .createdAt(LocalDateTime.now())
                .build();

        String json = objectMapper.writeValueAsString(dto);
        OrderDTO deserialized = objectMapper.readValue(json, OrderDTO.class);

        assertEquals(OrderStatus.CONFIRMED, deserialized.getStatus());
        assertEquals(OrderType.DELIVERY, deserialized.getOrderType());
        assertEquals(0, new BigDecimal("25.50").compareTo(deserialized.getTotalPrice()));
    }

    @Test
    void testUserDtoWithRole() throws Exception {
        UserDTO dto = UserDTO.builder()
                .id(1L)
                .username("admin")
                .email("admin@test.com")
                .fullName("Admin User")
                .role(Role.ADMIN)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();

        String json = objectMapper.writeValueAsString(dto);
        UserDTO deserialized = objectMapper.readValue(json, UserDTO.class);

        assertEquals(Role.ADMIN, deserialized.getRole());
        assertEquals("admin", deserialized.getUsername());
    }

    @Test
    void testProductDto() throws Exception {
        CategoryDTO category = CategoryDTO.builder()
                .id(1L)
                .name("Burgers")
                .build();

        ProductDTO dto = ProductDTO.builder()
                .id(1L)
                .name("Cheeseburger")
                .description("Delicious burger")
                .price(new BigDecimal("9.99"))
                .available(true)
                .category(category)
                .build();

        String json = objectMapper.writeValueAsString(dto);
        ProductDTO deserialized = objectMapper.readValue(json, ProductDTO.class);

        assertEquals("Cheeseburger", deserialized.getName());
        assertEquals("Burgers", deserialized.getCategory().getName());
    }

    @Test
    void testCreateOrderRequestDto() throws Exception {
        CreateOrderItemRequestDTO item = CreateOrderItemRequestDTO.builder()
                .productId(1L)
                .quantity(2)
                .price(new BigDecimal("9.99"))
                .build();

        CreateOrderRequestDTO dto = CreateOrderRequestDTO.builder()
                .customerName("Jane Doe")
                .customerPhone("555-5678")
                .customerAddress("456 Main St")
                .totalAmount(new BigDecimal("19.98"))
                .orderType(OrderType.DELIVERY)
                .items(Arrays.asList(item))
                .build();

        String json = objectMapper.writeValueAsString(dto);
        CreateOrderRequestDTO deserialized = objectMapper.readValue(json, CreateOrderRequestDTO.class);

        assertEquals("Jane Doe", deserialized.getCustomerName());
        assertEquals(OrderType.DELIVERY, deserialized.getOrderType());
        assertEquals(1, deserialized.getItems().size());
    }

    @Test
    void testUpdateOrderStatusRequestDto() throws Exception {
        UpdateOrderStatusRequestDTO dto = UpdateOrderStatusRequestDTO.builder()
                .status(OrderStatus.PREPARING)
                .build();

        String json = objectMapper.writeValueAsString(dto);
        UpdateOrderStatusRequestDTO deserialized = objectMapper.readValue(json, UpdateOrderStatusRequestDTO.class);

        assertEquals(OrderStatus.PREPARING, deserialized.getStatus());
    }

    @Test
    void testUpdateDeliveryStatusRequestDto() throws Exception {
        UpdateDeliveryStatusRequestDTO dto = UpdateDeliveryStatusRequestDTO.builder()
                .status(DeliveryStatus.ON_THE_WAY)
                .build();

        String json = objectMapper.writeValueAsString(dto);
        UpdateDeliveryStatusRequestDTO deserialized = objectMapper.readValue(json, UpdateDeliveryStatusRequestDTO.class);

        assertEquals(DeliveryStatus.ON_THE_WAY, deserialized.getStatus());
    }

    @Test
    void testRegisterRequestDto() throws Exception {
        RegisterRequestDTO dto = RegisterRequestDTO.builder()
                .username("newuser")
                .password("password123")
                .email("newuser@test.com")
                .fullName("New User")
                .role(Role.KITCHEN_STAFF)
                .build();

        String json = objectMapper.writeValueAsString(dto);
        RegisterRequestDTO deserialized = objectMapper.readValue(json, RegisterRequestDTO.class);

        assertEquals(Role.KITCHEN_STAFF, deserialized.getRole());
        assertEquals("newuser", deserialized.getUsername());
    }

    @Test
    void testUpdateRoleRequestDto() throws Exception {
        UpdateRoleRequestDTO dto = UpdateRoleRequestDTO.builder()
                .role(Role.MANAGER)
                .build();

        String json = objectMapper.writeValueAsString(dto);
        UpdateRoleRequestDTO deserialized = objectMapper.readValue(json, UpdateRoleRequestDTO.class);

        assertEquals(Role.MANAGER, deserialized.getRole());
    }

    @Test
    void testApiResponseDto() throws Exception {
        ApiResponseDTO<String> dto = ApiResponseDTO.success("Test message", "data");

        String json = objectMapper.writeValueAsString(dto);

        assertTrue(json.contains("success"));
        assertTrue(json.contains("Test message"));
        assertTrue(json.contains("data"));
    }

    @Test
    void testTokenResponse() throws Exception {
        UserDTO user = UserDTO.builder()
                .id(1L)
                .username("admin")
                .role(Role.ADMIN)
                .build();

        TokenResponse dto = TokenResponse.of("test-token", user);

        String json = objectMapper.writeValueAsString(dto);
        TokenResponse deserialized = objectMapper.readValue(json, TokenResponse.class);

        assertEquals("test-token", deserialized.getToken());
        assertEquals("admin", deserialized.getUser().getUsername());
        assertEquals(Role.ADMIN, deserialized.getUser().getRole());
    }

    @Test
    void testWebSocketMessageDto() throws Exception {
        WebSocketMessageDTO dto = WebSocketMessageDTO.builder()
                .type("ORDER_UPDATED")
                .message("Order status changed")
                .data("test-data")
                .timestamp(LocalDateTime.now())
                .build();

        String json = objectMapper.writeValueAsString(dto);
        WebSocketMessageDTO deserialized = objectMapper.readValue(json, WebSocketMessageDTO.class);

        assertEquals("ORDER_UPDATED", deserialized.getType());
        assertEquals("Order status changed", deserialized.getMessage());
    }

    @Test
    void testAllDeliveryStatuses() {
        DeliveryStatus[] statuses = DeliveryStatus.values();
        assertEquals(6, statuses.length);
        assertTrue(Arrays.asList(statuses).contains(DeliveryStatus.PENDING));
        assertTrue(Arrays.asList(statuses).contains(DeliveryStatus.ASSIGNED));
        assertTrue(Arrays.asList(statuses).contains(DeliveryStatus.PICKED_UP));
        assertTrue(Arrays.asList(statuses).contains(DeliveryStatus.ON_THE_WAY));
        assertTrue(Arrays.asList(statuses).contains(DeliveryStatus.DELIVERED));
        assertTrue(Arrays.asList(statuses).contains(DeliveryStatus.CANCELLED));
    }

    @Test
    void testAllOrderStatuses() {
        OrderStatus[] statuses = OrderStatus.values();
        assertEquals(7, statuses.length);
        assertTrue(Arrays.asList(statuses).contains(OrderStatus.PENDING));
        assertTrue(Arrays.asList(statuses).contains(OrderStatus.CONFIRMED));
        assertTrue(Arrays.asList(statuses).contains(OrderStatus.PREPARING));
        assertTrue(Arrays.asList(statuses).contains(OrderStatus.READY_FOR_DELIVERY));
        assertTrue(Arrays.asList(statuses).contains(OrderStatus.OUT_FOR_DELIVERY));
        assertTrue(Arrays.asList(statuses).contains(OrderStatus.COMPLETED));
        assertTrue(Arrays.asList(statuses).contains(OrderStatus.CANCELLED));
    }

    @Test
    void testAllOrderTypes() {
        OrderType[] types = OrderType.values();
        assertEquals(3, types.length);
        assertTrue(Arrays.asList(types).contains(OrderType.DELIVERY));
        assertTrue(Arrays.asList(types).contains(OrderType.PICKUP));
        assertTrue(Arrays.asList(types).contains(OrderType.DINE_IN));
    }

    @Test
    void testAllRoles() {
        Role[] roles = Role.values();
        assertEquals(4, roles.length);
        assertTrue(Arrays.asList(roles).contains(Role.ADMIN));
        assertTrue(Arrays.asList(roles).contains(Role.MANAGER));
        assertTrue(Arrays.asList(roles).contains(Role.KITCHEN_STAFF));
        assertTrue(Arrays.asList(roles).contains(Role.DELIVERY_STAFF));
    }

    @Test
    void testCreateOrderRequestDefaultOrderType() {
        // Test with address - should default to DELIVERY
        CreateOrderRequestDTO withAddress = CreateOrderRequestDTO.builder()
                .customerName("Test")
                .customerPhone("555-0000")
                .customerAddress("123 St")
                .totalAmount(BigDecimal.TEN)
                .items(Arrays.asList())
                .build();

        assertEquals(OrderType.DELIVERY, withAddress.getEffectiveOrderType());

        // Test without address - should default to PICKUP
        CreateOrderRequestDTO withoutAddress = CreateOrderRequestDTO.builder()
                .customerName("Test")
                .customerPhone("555-0000")
                .totalAmount(BigDecimal.TEN)
                .items(Arrays.asList())
                .build();

        assertEquals(OrderType.PICKUP, withoutAddress.getEffectiveOrderType());
    }

    @Test
    void testLoginRequestDto() throws Exception {
        LoginRequestDTO dto = LoginRequestDTO.builder()
                .username("testuser")
                .password("testpass")
                .build();

        String json = objectMapper.writeValueAsString(dto);
        LoginRequestDTO deserialized = objectMapper.readValue(json, LoginRequestDTO.class);

        assertEquals("testuser", deserialized.getUsername());
        assertEquals("testpass", deserialized.getPassword());
    }

    @Test
    void testAssignDeliveryRequestDto() throws Exception {
        AssignDeliveryRequestDTO dto = AssignDeliveryRequestDTO.builder()
                .orderId(1L)
                .driverId(5L)
                .deliveryAddress("789 Delivery Ave")
                .deliveryNotes("Ring bell twice")
                .build();

        String json = objectMapper.writeValueAsString(dto);
        AssignDeliveryRequestDTO deserialized = objectMapper.readValue(json, AssignDeliveryRequestDTO.class);

        assertEquals(1L, deserialized.getOrderId());
        assertEquals(5L, deserialized.getDriverId());
        assertEquals("789 Delivery Ave", deserialized.getDeliveryAddress());
    }

    @Test
    void testChangePasswordRequestDto() throws Exception {
        ChangePasswordRequestDTO dto = ChangePasswordRequestDTO.builder()
                .username("user")
                .currentPassword("oldpass")
                .newPassword("newpass")
                .build();

        String json = objectMapper.writeValueAsString(dto);
        ChangePasswordRequestDTO deserialized = objectMapper.readValue(json, ChangePasswordRequestDTO.class);

        assertEquals("user", deserialized.getUsername());
        assertEquals("oldpass", deserialized.getCurrentPassword());
        assertEquals("newpass", deserialized.getNewPassword());
    }

    @Test
    void testPagedResponseDto() throws Exception {
        PagedResponseDTO<String> dto = PagedResponseDTO.<String>builder()
                .content(Arrays.asList("item1", "item2"))
                .page(0)
                .size(10)
                .totalElements(2L)
                .totalPages(1)
                .first(true)
                .last(true)
                .hasNext(false)
                .hasPrevious(false)
                .build();

        String json = objectMapper.writeValueAsString(dto);
        assertNotNull(json);
        assertTrue(json.contains("item1"));
        assertTrue(json.contains("item2"));
    }

    @Test
    void testStatsResponseDto() throws Exception {
        StatsResponseDTO dto = StatsResponseDTO.builder()
                .orderCount(100L)
                .revenue(new BigDecimal("5000.00"))
                .completedCount(85L)
                .activeCount(15L)
                .build();

        String json = objectMapper.writeValueAsString(dto);
        StatsResponseDTO deserialized = objectMapper.readValue(json, StatsResponseDTO.class);

        assertEquals(100L, deserialized.getOrderCount());
        assertEquals(0, new BigDecimal("5000.00").compareTo(deserialized.getRevenue()));
    }
}
