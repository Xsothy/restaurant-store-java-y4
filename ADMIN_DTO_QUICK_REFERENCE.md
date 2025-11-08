# Admin DTO Quick Reference

## Overview
The `com.restaurant.store.dto.admin` package contains type-safe DTOs for integrating with the restaurant admin backend.

## Package Structure

```
com.restaurant.store.dto.admin/
├── Main DTOs (entities)
│   ├── CategoryDTO.java          - Menu category
│   ├── DeliveryDTO.java          - Delivery information
│   ├── OrderDTO.java             - Order details
│   ├── OrderItemDTO.java         - Order line items
│   ├── ProductDTO.java           - Menu products
│   └── UserDTO.java              - Admin users (staff)
│
├── request/ (API request DTOs)
│   ├── AssignDeliveryRequestDTO.java
│   ├── ChangePasswordRequestDTO.java
│   ├── CreateOrderItemRequestDTO.java
│   ├── CreateOrderRequestDTO.java
│   ├── LoginRequestDTO.java
│   ├── ReassignDriverRequestDTO.java
│   ├── RegisterRequestDTO.java
│   ├── UpdateDeliveryRequestDTO.java
│   ├── UpdateDeliveryStatusRequestDTO.java
│   ├── UpdateOrderStatusRequestDTO.java
│   └── UpdateRoleRequestDTO.java
│
├── response/ (API response DTOs)
│   ├── ApiResponseDTO.java       - Generic API response wrapper
│   ├── PagedResponseDTO.java     - Paginated results
│   ├── StatsResponseDTO.java     - Statistics data
│   └── TokenResponse.java        - Authentication token + user
│
└── websocket/
    └── WebSocketMessageDTO.java  - Real-time updates
```

## Entity Enums

### DeliveryStatus
```java
import com.restaurant.store.entity.DeliveryStatus;

DeliveryStatus.PENDING
DeliveryStatus.ASSIGNED
DeliveryStatus.PICKED_UP
DeliveryStatus.ON_THE_WAY
DeliveryStatus.DELIVERED
DeliveryStatus.CANCELLED
```

### OrderStatus
```java
import com.restaurant.store.entity.OrderStatus;

OrderStatus.PENDING
OrderStatus.CONFIRMED
OrderStatus.PREPARING
OrderStatus.READY
OrderStatus.OUT_FOR_DELIVERY
OrderStatus.DELIVERED
OrderStatus.CANCELLED
```

### OrderType
```java
import com.restaurant.store.entity.OrderType;

OrderType.DELIVERY
OrderType.PICKUP
OrderType.DINE_IN
```

### Role
```java
import com.restaurant.store.entity.Role;

Role.ADMIN
Role.MANAGER
Role.CHEF
Role.DRIVER
```

## Usage Examples

### Creating an Order Request
```java
import com.restaurant.store.dto.admin.request.CreateOrderRequestDTO;
import com.restaurant.store.dto.admin.request.CreateOrderItemRequestDTO;
import com.restaurant.store.entity.OrderType;

CreateOrderItemRequestDTO item = CreateOrderItemRequestDTO.builder()
    .productId(1L)
    .quantity(2)
    .price(new BigDecimal("9.99"))
    .build();

CreateOrderRequestDTO order = CreateOrderRequestDTO.builder()
    .customerName("John Doe")
    .customerPhone("555-1234")
    .customerAddress("123 Main St")
    .totalAmount(new BigDecimal("19.98"))
    .orderType(OrderType.DELIVERY)
    .items(List.of(item))
    .build();

// Get formatted customer details
String details = order.getCustomerDetails();
// Output: "Name: John Doe | Phone: 555-1234 | Address: 123 Main St"

// Get effective order type (auto-determines if not set)
OrderType type = order.getEffectiveOrderType();
```

### Updating Order Status
```java
import com.restaurant.store.dto.admin.request.UpdateOrderStatusRequestDTO;
import com.restaurant.store.entity.OrderStatus;

UpdateOrderStatusRequestDTO request = UpdateOrderStatusRequestDTO.builder()
    .status(OrderStatus.PREPARING)
    .build();
```

### User Registration with Role
```java
import com.restaurant.store.dto.admin.request.RegisterRequestDTO;
import com.restaurant.store.entity.Role;

RegisterRequestDTO request = RegisterRequestDTO.builder()
    .username("chef1")
    .password("securepass")
    .email("chef1@restaurant.com")
    .fullName("Chef John")
    .role(Role.CHEF)
    .build();
```

### API Response Wrapper
```java
import com.restaurant.store.dto.admin.response.ApiResponseDTO;

// Success response
ApiResponseDTO<OrderDTO> response = ApiResponseDTO.success(
    "Order created successfully",
    orderDto
);

// Error response
ApiResponseDTO<Void> errorResponse = ApiResponseDTO.error(
    "Order not found",
    "No order exists with ID 123"
);
```

### Token Response
```java
import com.restaurant.store.dto.admin.response.TokenResponse;
import com.restaurant.store.dto.admin.UserDTO;

TokenResponse response = TokenResponse.of(jwtToken, userDto);
```

## JSON Examples

### OrderDTO
```json
{
  "id": 1,
  "customerName": "John Doe",
  "customerPhone": "555-1234",
  "customerAddress": "123 Main St",
  "customerDetails": "Name: John Doe | Phone: 555-1234 | Address: 123 Main St",
  "status": "CONFIRMED",
  "orderType": "DELIVERY",
  "totalPrice": 25.50,
  "createdAt": "2024-01-01T12:00:00",
  "orderItems": [...]
}
```

### DeliveryDTO
```json
{
  "id": 1,
  "orderId": 100,
  "status": "ON_THE_WAY",
  "deliveryAddress": "123 Main St",
  "dispatchedAt": "2024-01-01T13:00:00"
}
```

### UserDTO
```json
{
  "id": 1,
  "username": "chef1",
  "email": "chef1@restaurant.com",
  "fullName": "Chef John",
  "role": "CHEF",
  "enabled": true,
  "createdAt": "2024-01-01T10:00:00"
}
```

## Validation

All request DTOs have Jakarta validation annotations:

```java
@NotBlank(message = "Customer name is required")
@Size(max = 100, message = "Customer name must not exceed 100 characters")
private String customerName;

@NotNull(message = "Total amount is required")
@DecimalMin(value = "0.0", inclusive = false, message = "Total amount must be greater than 0")
private BigDecimal totalAmount;
```

## Integration Points

### 1. Admin API Client
Use these DTOs when making REST calls to the admin backend:
```java
// Send order to admin backend
ApiResponseDTO<OrderDTO> response = adminApiClient.post("/orders", createOrderRequest);
```

### 2. WebSocket Messages
Subscribe to real-time updates:
```java
WebSocketMessageDTO message = WebSocketMessageDTO.builder()
    .type("ORDER_STATUS_CHANGED")
    .message("Order #123 is now being prepared")
    .data(orderDto)
    .timestamp(LocalDateTime.now())
    .build();
```

### 3. Internal API
Receive updates from admin backend:
```java
@PostMapping("/api/internal/orders/{id}/status")
public ResponseEntity<ApiResponse> updateOrderStatus(
    @PathVariable Long id,
    @RequestBody UpdateOrderStatusRequestDTO request) {
    // Handle status update
}
```

## Important Notes

1. **No External Dependencies**: These DTOs use only store project entities
2. **JSON Compatible**: All DTOs serialize/deserialize correctly with Jackson
3. **Type Safe**: Full compile-time type checking with enums
4. **Validation Ready**: Jakarta validation annotations included
5. **Builder Pattern**: All DTOs support builder pattern for easy construction

## Testing

Run the test suite:
```bash
# Quick verification
./test-admin-dtos.sh

# Unit tests
mvn test -Dtest=AdminDtoSimpleTest

# Full build
mvn clean compile
```

## Support

For issues or questions:
1. Check `ADMIN_DTO_TEST_REPORT.md` for detailed test results
2. Review `TEST_SUMMARY.md` for quick overview
3. Ensure all imports use `com.restaurant.store.entity.*` (not `com.resadmin.*`)
