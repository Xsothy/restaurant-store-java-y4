# API Documentation

## Overview

This document outlines the separation between **Open APIs** (publicly accessible) and **Private APIs** (internal use only) in the Restaurant Store API.

## API Categories

### 1. Open APIs (Public)

These endpoints are designed for customer-facing applications (mobile apps, web clients) and are part of the public REST API.

#### Authentication APIs
- **Base Path**: `/api/auth`
- **Access**: Public (Register, Login) / Authenticated (Logout)
- **Endpoints**:
  - `POST /api/auth/register` - Register a new customer
  - `POST /api/auth/login` - Login and receive JWT token
  - `POST /api/auth/logout` - Logout (requires JWT token)

#### Product & Category APIs
- **Base Path**: `/api/products`, `/api/categories`
- **Access**: Public
- **Endpoints**:
  - `GET /api/categories` - Get all categories
  - `GET /api/products` - Get all products (with optional filters)
  - `GET /api/products/{productId}` - Get product details
  - `GET /api/categories/{categoryId}/products` - Get products by category

#### Cart APIs
- **Base Path**: `/api/cart`
- **Access**: Authenticated (requires JWT token)
- **Endpoints**:
  - `GET /api/cart` - Get current user's cart
  - `POST /api/cart/add` - Add item to cart
  - `PUT /api/cart/items/{cartItemId}` - Update cart item quantity
  - `DELETE /api/cart/items/{cartItemId}` - Remove item from cart
  - `DELETE /api/cart/clear` - Clear entire cart

#### Order APIs
- **Base Path**: `/api/orders`
- **Access**: Authenticated (requires JWT token)
- **Endpoints**:
  - `POST /api/orders` - Create new order from cart
  - `GET /api/orders/{orderId}` - Get order details
  - `GET /api/orders/{orderId}/status` - Get order status
  - `GET /api/orders/my-orders` - Get current user's orders
  - `PUT /api/orders/{orderId}/cancel` - Cancel order
  - `POST /api/orders/{orderId}/payment-intent` - Create Stripe payment intent
  - `POST /api/orders/{orderId}/pay` - Process payment

#### Delivery APIs
- **Base Path**: `/api/deliveries`
- **Access**: Authenticated (requires JWT token)
- **Endpoints**:
  - `GET /api/deliveries/order/{orderId}` - Get delivery information for an order

#### Webhook APIs
- **Base Path**: `/api/webhooks`
- **Access**: Public (validated via Stripe signature)
- **Endpoints**:
  - `POST /api/webhooks/stripe` - Stripe webhook for payment events

#### WebSocket APIs
- **Path**: `/ws`
- **Access**: Public
- **Topics**:
  - `/topic/orders/{orderId}` - Subscribe to order status updates

---

### 2. Private APIs (Internal Only)

These endpoints are designed for internal system communication and should NOT be exposed to public clients. In production, these should be secured with:
- API Key authentication
- IP whitelisting
- Network-level restrictions (internal network only)
- Service mesh authentication

#### Internal Order APIs
- **Base Path**: `/api/internal`
- **Access**: Internal systems only (Admin Backend)
- **Package**: `com.restaurant.store.controller.api.internal`
- **Hidden from Swagger**: Yes (`@Hidden` annotation)
- **Endpoints**:
  - `POST /api/internal/orders/{orderId}/status` - Update order status from Admin Backend
  - `POST /api/internal/orders/{orderId}/sync` - Sync order with external system ID

#### Data Synchronization APIs
- **Base Path**: `/api/sync`
- **Access**: Internal systems only
- **Package**: `com.restaurant.store.controller.api.internal`
- **Hidden from Swagger**: Yes (`@Hidden` annotation)
- **Endpoints**:
  - `POST /api/sync/all` - Sync all data from Admin Backend
  - `POST /api/sync/categories` - Sync categories only
  - `POST /api/sync/products` - Sync products only

---

## Security Configuration

### Open API Security

1. **Public Endpoints**: No authentication required
   - Authentication (register, login)
   - Product browsing
   - Category browsing
   - Webhooks (validated via signature)

2. **Authenticated Endpoints**: Require JWT Bearer token
   - Cart operations
   - Order operations
   - Delivery tracking
   - Logout

3. **JWT Authentication**:
   - Token obtained via `/api/auth/login`
   - Token must be included in `Authorization` header: `Bearer <token>`
   - Token expiration: Configured in `jwt.expiration` property

### Private API Security

⚠️ **IMPORTANT**: Private APIs are currently accessible without authentication. This is acceptable for development/testing but MUST be secured in production.

#### Recommended Security Measures for Production:

1. **API Key Authentication**:
   ```java
   @PreAuthorize("hasAuthority('INTERNAL_SERVICE')")
   ```

2. **IP Whitelisting**:
   ```properties
   internal.api.allowed.ips=10.0.0.0/8,172.16.0.0/12
   ```

3. **Network Isolation**:
   - Deploy private APIs on internal network only
   - Use service mesh (Istio, Linkerd) for service-to-service authentication

4. **Rate Limiting**:
   - Implement rate limiting for private endpoints
   - Monitor for unusual access patterns

---

## Testing

### Running Integration Tests

All open APIs have comprehensive integration tests to ensure functionality:

```bash
# Run all API tests
mvn test

# Run specific test class
mvn test -Dtest=AuthControllerIntegrationTest
mvn test -Dtest=ProductControllerIntegrationTest
mvn test -Dtest=CartControllerIntegrationTest
mvn test -Dtest=OrderControllerIntegrationTest
```

### Test Coverage

#### AuthController Tests
- ✅ Register new customer
- ✅ Register with existing email (409 Conflict)
- ✅ Register with invalid data (400 Bad Request)
- ✅ Login with valid credentials
- ✅ Login with invalid credentials (401 Unauthorized)
- ✅ Login with non-existent email (401 Unauthorized)
- ✅ Logout with valid token
- ✅ Logout without token (401 Unauthorized)

#### ProductController Tests
- ✅ Get all categories
- ✅ Get all products
- ✅ Get available products only
- ✅ Get products by category
- ✅ Get available products by category
- ✅ Get product by ID
- ✅ Get product by ID (404 Not Found)
- ✅ Get products by category endpoint
- ✅ Get products for non-existent category (404 Not Found)
- ✅ Get products for empty category

#### CartController Tests
- ✅ Get empty cart for new user
- ✅ Add item to cart
- ✅ Add multiple items to cart
- ✅ Add non-existent product (404 Not Found)
- ✅ Add to cart without authentication (401 Unauthorized)
- ✅ Update cart item quantity
- ✅ Remove item from cart
- ✅ Clear cart
- ✅ Update non-existent cart item (404 Not Found)
- ✅ Remove non-existent cart item (404 Not Found)

#### OrderController Tests
- ✅ Create order from cart
- ✅ Create order with empty cart (400 Bad Request)
- ✅ Create order without authentication (401 Unauthorized)
- ✅ Get order by ID
- ✅ Get non-existent order (404 Not Found)
- ✅ Get order status
- ✅ Get my orders
- ✅ Get my orders (empty list)
- ✅ Cancel order
- ✅ Cancel non-existent order (404 Not Found)
- ✅ Create payment intent

---

## API Documentation (Swagger/OpenAPI)

### Accessing Swagger UI

1. Start the application:
   ```bash
   mvn spring-boot:run
   ```

2. Open Swagger UI in browser:
   ```
   http://localhost:8080/swagger-ui.html
   ```

3. View OpenAPI JSON specification:
   ```
   http://localhost:8080/v3/api-docs
   ```

### Using Authentication in Swagger

1. Execute `/api/auth/login` endpoint to get JWT token
2. Copy the token from response
3. Click "Authorize" button in Swagger UI
4. Enter: `Bearer <your-token>`
5. Click "Authorize"
6. All authenticated endpoints will now include the token

### Private APIs in Swagger

Private APIs are hidden from Swagger documentation using the `@Hidden` annotation. This prevents them from appearing in the public API documentation.

---

## Error Handling

All APIs return consistent error responses:

```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Detailed error message",
  "path": "/api/endpoint",
  "validationErrors": {
    "fieldName": "Error message for specific field"
  }
}
```

### HTTP Status Codes

- `200 OK` - Successful request
- `201 Created` - Resource created successfully
- `400 Bad Request` - Invalid request data
- `401 Unauthorized` - Missing or invalid authentication
- `403 Forbidden` - Access denied
- `404 Not Found` - Resource not found
- `409 Conflict` - Resource conflict (e.g., duplicate email)
- `500 Internal Server Error` - Server error

---

## Rate Limiting (Recommended)

For production deployments, implement rate limiting:

- **Public endpoints**: 100 requests/minute per IP
- **Authenticated endpoints**: 1000 requests/minute per user
- **Private endpoints**: 10000 requests/minute (internal only)

---

## Monitoring and Logging

### API Metrics

Enable Spring Boot Actuator for monitoring:
```properties
management.endpoints.web.exposure.include=health,metrics,info
```

### Access Logs

API access is logged at DEBUG level:
```properties
logging.level.com.restaurant.store.controller=DEBUG
```

### Alert Conditions

Set up alerts for:
- High error rates (>5% of requests)
- Unauthorized access attempts to private APIs
- Payment processing failures
- Order creation failures

---

## Migration from Monolithic to Microservices

If migrating to microservices architecture:

1. **API Gateway**: Route public APIs through API gateway
2. **Service Mesh**: Use service mesh for private API communication
3. **Authentication**: Centralize authentication in auth service
4. **Authorization**: Implement fine-grained authorization policies
5. **Circuit Breakers**: Add resilience patterns for service communication

---

## Contact

For questions or issues regarding the API:
- Create an issue in the project repository
- Contact the development team
- Review the source code in `src/main/java/com/restaurant/store/controller/api/`
