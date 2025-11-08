# Controller Structure

This document describes the organized controller architecture of the restaurant store application.

## Directory Structure

```
src/main/java/com/restaurant/store/controller/
├── api/                          # REST API Controllers (JSON responses)
│   ├── AdminSyncController.java
│   ├── AuthController.java
│   ├── CartController.java
│   ├── DeliveryController.java
│   ├── InternalApiController.java
│   ├── OrderController.java
│   ├── OrderStatusWebSocketController.java
│   ├── ProductController.java
│   └── StripeWebhookController.java
│
└── web/                          # Web Controllers (HTML views)
    ├── AuthWebController.java
    ├── MenuController.java
    ├── OrderWebController.java
    ├── WebController.java
    └── WebPaymentController.java
```

## API Controllers (`controller/api`)

### AuthController
- **Package**: `com.restaurant.store.controller.api`
- **Base Path**: `/api/auth`
- **Purpose**: Authentication and user registration
- **Endpoints**:
  - POST `/register` - Register new customer
  - POST `/login` - Login with email and password
  - POST `/logout` - Logout endpoint

### CartController
- **Package**: `com.restaurant.store.controller.api`
- **Base Path**: `/api/cart`
- **Purpose**: Shopping cart management
- **Endpoints**:
  - GET `/` - Get current user's cart
  - POST `/add` - Add item to cart
  - PUT `/items/{cartItemId}` - Update cart item quantity
  - DELETE `/items/{cartItemId}` - Remove item from cart
  - DELETE `/clear` - Clear entire cart

### OrderController
- **Package**: `com.restaurant.store.controller.api`
- **Base Path**: `/api/orders`
- **Purpose**: Order management and processing
- **Endpoints**:
  - POST `/` - Create new order
  - GET `/{orderId}` - Get order details
  - GET `/{orderId}/status` - Get order status
  - POST `/{orderId}/payment-intent` - Create payment intent
  - POST `/{orderId}/pay` - Process payment
  - GET `/customer/{customerId}` - Get customer orders
  - GET `/my-orders` - Get authenticated user's orders
  - PUT `/{orderId}/cancel` - Cancel order

### ProductController
- **Package**: `com.restaurant.store.controller.api`
- **Base Path**: `/api`
- **Purpose**: Product and category browsing
- **Endpoints**:
  - GET `/categories` - Get all categories
  - GET `/products` - Get all products (with filters)
  - GET `/products/{productId}` - Get product details
  - GET `/categories/{categoryId}/products` - Get products by category

### DeliveryController
- **Package**: `com.restaurant.store.controller.api`
- **Base Path**: `/api/deliveries`
- **Purpose**: Delivery tracking
- **Endpoints**:
  - GET `/{orderId}` - Get delivery info by order
  - GET `/track/{orderId}` - Track delivery
  - PUT `/{deliveryId}/update-location` - Update delivery location
  - PUT `/{deliveryId}/update-status` - Update delivery status

### AdminSyncController
- **Package**: `com.restaurant.store.controller.api`
- **Base Path**: `/api/sync`
- **Purpose**: Manual data synchronization with Admin Backend
- **Endpoints**:
  - POST `/all` - Sync all data
  - POST `/categories` - Sync categories only
  - POST `/products` - Sync products only

### InternalApiController
- **Package**: `com.restaurant.store.controller.api`
- **Base Path**: `/api/internal`
- **Purpose**: Internal API for Admin Backend integration
- **Endpoints**:
  - POST `/orders/{orderId}/status` - Update order status
  - POST `/orders/{orderId}/sync` - Sync order with external ID

### StripeWebhookController
- **Package**: `com.restaurant.store.controller.api`
- **Base Path**: `/api/webhooks`
- **Purpose**: Handle Stripe payment webhooks
- **Endpoints**:
  - POST `/stripe` - Stripe webhook handler

### OrderStatusWebSocketController
- **Package**: `com.restaurant.store.controller.api`
- **Purpose**: WebSocket for real-time order updates
- **Endpoints**:
  - WebSocket `/ws` - Connection endpoint
  - Topic `/topic/orders/{orderId}` - Subscribe to order updates

## Web Controllers (`controller/web`)

### WebController
- **Package**: `com.restaurant.store.controller.web`
- **Purpose**: Main web pages with server-side rendering
- **Endpoints**:
  - GET `/login` - Login page
  - GET `/register` - Registration page
  - GET `/cart` - Cart page
  - GET `/checkout` - Checkout page
  - GET `/profile` - User profile page
  - GET `/payment/success` - Payment success page
  - GET `/payment/cancel` - Payment cancel page

### MenuController
- **Package**: `com.restaurant.store.controller.web`
- **Purpose**: Menu browsing pages
- **Endpoints**:
  - GET `/` - Home/menu page
  - GET `/menu` - Menu page
  - GET `/products/{productId}` - Product details page

### AuthWebController
- **Package**: `com.restaurant.store.controller.web`
- **Base Path**: `/auth`
- **Purpose**: Web authentication forms
- **Endpoints**:
  - GET `/login` - Login form
  - GET `/register` - Registration form
  - POST `/login` - Process login
  - POST `/register` - Process registration
  - POST `/logout` - Process logout

### OrderWebController
- **Package**: `com.restaurant.store.controller.web`
- **Base Path**: `/orders`
- **Purpose**: Order viewing pages
- **Endpoints**:
  - GET `/` - Orders list page
  - GET `/{orderId}` - Order details page

### WebPaymentController
- **Package**: `com.restaurant.store.controller.web`
- **Base Path**: `/api/web/payment`
- **Purpose**: Web payment processing (supports both Payment Intent and Session)
- **Endpoints**:
  - POST `/create/{orderId}` - Create payment for order

## Design Principles

1. **Clear Separation**: API controllers handle JSON/REST, web controllers handle HTML views
2. **Package Organization**: Controllers grouped by responsibility (api vs web)
3. **Consistent Naming**: Controllers follow naming conventions (Controller suffix for API, WebController suffix for web)
4. **Single Responsibility**: Each controller has a focused purpose
5. **RESTful Design**: API endpoints follow REST conventions
6. **Server-Side Rendering**: Web controllers pass data via Model to templates

## Adding New Controllers

### For API Controllers:
1. Place in `com.restaurant.store.controller.api` package
2. Annotate with `@RestController`
3. Use `@RequestMapping("/api/...")` for base path
4. Return `ResponseEntity<ApiResponse<T>>` for consistent responses

### For Web Controllers:
1. Place in `com.restaurant.store.controller.web` package
2. Annotate with `@Controller`
3. Return view names (String) or use `Model` to pass data
4. Follow server-side rendering pattern

## Migration Notes

All controllers have been successfully reorganized from the flat structure to the hierarchical structure. The project compiles successfully with no breaking changes to the API or web routes.
