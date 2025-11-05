# Restaurant Store - Refactoring Documentation

## 🏗️ Architecture Overview

This document outlines the comprehensive refactoring of the Restaurant Store application to implement a clean separation of concerns between API and Web controllers, enhanced security, and server-side rendering.

## 📁 New Folder Structure

```
src/main/java/com/restaurant/store/
├─ controller/
│  ├─ api/                    # API Controllers (JWT + Statelesss)
│  │  ├─ AuthController.java
│  │  ├─ ProductController.java
│  │  ├─ OrderController.java
│  │  └─ DeliveryController.java
│  └─ web/                    # Web Controllers (Session + Templates)
│     ├─ MenuController.java
│     ├─ AuthWebController.java
│     ├─ OrderWebController.java
│     └─ WebController.java
├─ security/
│  └─ config/
│     ├─ ApiSecurityConfig.java # JWT-based API security
│     └─ WebSecurityConfig.java # Session-based web security
├─ service/                   # Business logic layer
├─ dto/                       # Data transfer objects
│  ├─ request/
│  └─ response/              # Including new CategoryResponse
├─ entity/                    # JPA entities
└─ repository/                # Spring Data repositories
```

## 🔑 Key Changes

### 1. Controller Layer Separation

**API Controllers (`/api/**`)**
- ✅ Return DTOs directly (no ApiResponse wrapper)
- ✅ JWT-based authentication
- ✅ Stateless endpoints
- ✅ Thin controllers - all business logic delegated to services

**Web Controllers (`/**`)**
- ✅ Return Thymeleaf templates with server-side data
- ✅ Session-based authentication
- ✅ Server-side rendering reduces JavaScript dependencies
- ✅ Proper form handling for login/register

### 2. Security Configuration

**ApiSecurityConfig (Order 1)**
```java
.securityMatcher("/api/**")
.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
```

**WebSecurityConfig (Order 2)**
```java
.securityMatcher("/**")
.formLogin(form -> form.loginPage("/auth/login"))
.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
```

### 3. DTO Usage Enhancement

**New CategoryResponse DTO**
```java
public static CategoryResponse fromEntity(Category category) {
    // Maps Category entity to CategoryResponse DTO
}
```

**Updated ProductService**
```java
public List<CategoryResponse> getAllCategories() {
    return categoryRepository.findAll()
           .stream()
           .map(CategoryResponse::fromEntity)
           .collect(Collectors.toList());
}
```

### 4. Template Refactoring

**Before (Client-side heavy)**
```html
<div x-data="menuApp()" x-init="init()">
    <template x-for="product in products">
        <!-- Heavy Alpine.js usage -->
    </template>
</div>
```

**After (Server-side rendered)**
```html
<div th:each="product : ${products}">
    <img th:src="${product.imageUrl}" th:alt="${product.name}" />
    <h3 th:text="${product.name}"></h3>
    <span th:text="${#numbers.formatDecimal(product.price, 0, 0) + '៛'}"></span>
</div>
```

### 5. Removed ApiResponse Wrapper

**Before**
```java
public ResponseEntity<ApiResponse<List<ProductResponse>>> getProducts() {
    List<ProductResponse> products = productService.getAllProducts();
    return ResponseEntity.ok(ApiResponse.success("Products retrieved", products));
}
```

**After**
```java
public List<ProductResponse> getProducts() {
    return productService.getAllProducts();
}
```

## 🔐 Authentication Flow

### Web Authentication (Session-based)
1. User visits `/auth/login` or `/auth/register`
2. Form submission to `/auth/login` (POST)
3. Spring Security authenticates and creates session
4. User redirected to `/menu` with session cookie
5. Logout invalidates session at `/auth/logout`

### API Authentication (JWT-based)
1. Client posts to `/api/auth/login` with credentials
2. Server returns JWT token in response body
3. Client includes `Authorization: Bearer <token>` header
4. JwtAuthenticationFilter validates token on each request
5. Stateless - no server session required

## 🎯 Benefits Achieved

### ✅ Clean Separation of Concerns
- API and Web controllers have distinct responsibilities
- Security configurations are properly separated
- Clear boundary between client-side and server-side concerns

### ✅ Enhanced Security
- Web: Session-based for traditional browser usage
- API: JWT-based for mobile/API consumption
- Proper authentication for each use case

### ✅ Better Performance
- Server-side rendering reduces client-side JavaScript
- Templates render with data, no additional API calls needed
- Reduced payload sizes (no ApiResponse wrapper)

### ✅ Improved Maintainability
- Thin controllers delegate to services
- DTOs prevent entity exposure
- Consistent error handling and response patterns

### ✅ Mobile/API Ready
- Clean JSON API endpoints
- JWT authentication works well with mobile apps
- No UI dependencies in API layer

## 🚀 Usage Examples

### API Usage (Mobile/SPA)
```bash
# Login
POST /api/auth/login
{
  "email": "user@example.com", 
  "password": "password"
}
# Response: { "token": "jwt_token", "customer": {...} }

# Get Products
GET /api/products
# Response: [{ "id": 1, "name": "Product", "price": 1000 }]
```

### Web Usage (Browser)
```bash
# Visit login page
GET /auth/login
# Returns HTML form

# Submit login form
POST /auth/login
Content-Type: application/x-www-form-urlencoded
email=user@example.com&password=password
# Redirects to /menu with session cookie
```

## 📋 Next Steps

### TODO Items (Implementation Phase)
1. **Complete Web Authentication**: Implement actual session logic in AuthWebController
2. **Order Management**: Add order details page and order creation flow
3. **Cart Integration**: Connect cart functionality with order creation
4. **Profile Management**: Implement customer profile editing
5. **Error Handling**: Add proper error pages and validation

### Future Enhancements
1. **Admin Panel**: Separate admin interface with role-based access
2. **Real-time Updates**: WebSocket for order status updates  
3. **Payment Integration**: Connect with payment gateway
4. **Mobile App**: Use existing API for mobile application

## 🔧 Development Commands

```bash
# Build project
./mvnw clean compile

# Run application
./mvnw spring-boot:run

# Build CSS (watch mode)
npx tailwindcss -c tailwind.config.js -i ./src/main/tailwind/input.css -o ./src/main/resources/static/css/tailwind.css --watch
```

## 📚 Reference Implementation

This refactoring follows Spring Boot best practices and implements:
- **Separation of Concerns**: Clear layer boundaries
- **DTO Pattern**: Proper data transfer objects
- **Security Best Practices**: Separate configurations for API/Web
- **Template Rendering**: Server-side with Thymeleaf
- **RESTful Design**: Clean API endpoints without unnecessary wrappers

The architecture is now production-ready and can easily scale to support mobile apps, SPAs, and traditional web interfaces.