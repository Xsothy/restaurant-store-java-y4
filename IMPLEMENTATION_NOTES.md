# Restaurant Store - Implementation Notes

## Summary of Changes

This document summarizes the refactoring and enhancements made to the Restaurant Store application.

## Architecture Overview

### Backend API Structure
- **All REST API endpoints are prefixed with `/api`**:
  - `/api/auth/*` - Authentication endpoints (login, register)
  - `/api/products/*` - Product catalog endpoints
  - `/api/categories/*` - Category management
  - `/api/orders/*` - Order management (requires authentication)
  - `/api/deliveries/*` - Delivery tracking (requires authentication)

### Web Application Structure
- **Web pages served from root paths** using Thymeleaf templates:
  - `/` or `/login` → Login page
  - `/menu` → Menu/product listing page
  - `/products/{id}` → Product details page

### Frontend Technology
- **Tailwind CSS** (via CDN) for modern, responsive styling
- **Thymeleaf** for server-side rendering with Spring Boot integration
- **Vanilla JavaScript** for client-side interactions

## Key Improvements

### 1. DTOs with Lombok
All DTOs now use Lombok annotations for cleaner code:
- `@Data` - Generates getters, setters, toString, equals, and hashCode
- `@Builder` - Enables fluent builder pattern for responses
- `@NoArgsConstructor` / `@AllArgsConstructor` - Generate constructors

**Request DTOs**:
- `LoginRequest`
- `CustomerRegisterRequest`
- `CreateOrderRequest`
- `OrderItemRequest`
- `PaymentRequest`

**Response DTOs**:
- `AuthResponse`
- `CustomerResponse`
- `ProductResponse`
- `OrderResponse`
- `OrderItemResponse`
- `DeliveryResponse`
- `ApiResponse<T>` - Generic wrapper for all API responses

### 2. Entity Mappers
Dedicated mapper components convert entities to DTOs:
- `CustomerMapper` - Customer → CustomerResponse
- `ProductMapper` - Product → ProductResponse
- `OrderMapper` - Order → OrderResponse
- `OrderItemMapper` - OrderItem → OrderItemResponse  
- `DeliveryMapper` - Delivery → DeliveryResponse

All mappers are Spring `@Component` beans and use the builder pattern.

### 3. Security Configuration
Updated Spring Security configuration:
- Public access to web pages (`/`, `/login`, `/menu`, `/products/**`)
- Public access to static resources (CSS, JS, images, HTML)
- Public access to API auth endpoints (`/api/auth/**`)
- Public access to product/category browsing (`/api/products/**`, `/api/categories/**`)
- JWT authentication required for orders and deliveries

### 4. Web Pages with Thymeleaf
Three main pages implemented:
1. **Login** (`login.html`)
   - Modern gradient background
   - Form validation
   - Error/success message display
   - Stores JWT token in localStorage
   - Redirects to menu after successful login

2. **Menu** (`menu.html`)
   - Category filter dropdown
   - "Available only" checkbox filter
   - Responsive product grid
   - Click product card to view details
   - Shows logged-in customer name
   - Logout functionality

3. **Product Details** (`product-details.html`)
   - Large product image
   - Full product information
   - Availability status badge
   - "Add to Cart" button (placeholder)
   - Back to menu navigation

All pages use:
- Thymeleaf `xmlns:th` namespace
- `th:inline="javascript"` for URL generation
- Tailwind CSS utility classes
- Responsive design (mobile-first)

## How to Run

1. **Build the project**:
   ```bash
   ./mvnw clean install
   ```

2. **Run the application**:
   ```bash
   ./mvnw spring-boot:run
   ```

3. **Access the web application**:
   - Open browser to `http://localhost:8080/`
   - You'll be redirected to the login page
   - Use sample credentials from the seeded data

4. **Test the API**:
   ```bash
   # Login
   curl -X POST http://localhost:8080/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"john@example.com","password":"password123"}'
   
   # Get products
   curl http://localhost:8080/api/products
   
   # Get categories
   curl http://localhost:8080/api/categories
   ```

## Authentication Flow

1. User submits login form on `/login`
2. JavaScript sends POST to `/api/auth/login`
3. Backend validates credentials and returns JWT token
4. Frontend stores token in localStorage
5. Frontend redirects to `/menu`
6. For authenticated API calls, frontend includes token in Authorization header

## Next Steps / TODO

- [ ] Implement user registration page
- [ ] Add shopping cart functionality
- [ ] Implement order placement flow
- [ ] Add order history page
- [ ] Implement delivery tracking page
- [ ] Add admin panel for menu management
- [ ] Implement real payment processing
- [ ] Add unit and integration tests
- [ ] Set up CI/CD pipeline
- [ ] Deploy to production environment

## Technology Stack

- **Backend**: Spring Boot 3.2, Spring Security, Spring Data JPA
- **Database**: SQLite
- **Authentication**: JWT (JSON Web Tokens)
- **Template Engine**: Thymeleaf
- **Frontend**: Tailwind CSS, Vanilla JavaScript
- **Build Tool**: Maven
- **Java Version**: 17
- **Code Generation**: Lombok 1.18.32

## Development Notes

- Static resources are served from `src/main/resources/static/`
- Thymeleaf templates are in `src/main/resources/templates/`
- Database file: `restaurant_store.db` (created on first run)
- Database schema: `create-drop` mode (resets on restart)
- Sample data loaded from `src/main/resources/data.sql`
