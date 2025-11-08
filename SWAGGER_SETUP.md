# Swagger/OpenAPI Documentation Setup

## Overview

This project now includes comprehensive Swagger/OpenAPI documentation with JWT authentication support and best practice exception handling.

## Features Implemented

### 1. Swagger/OpenAPI Integration
- **SpringDoc OpenAPI 2.3.0** integrated for Spring Boot 3.2
- Interactive API documentation with Swagger UI
- JWT Bearer authentication support in Swagger UI
- Comprehensive API descriptions and examples

### 2. Enhanced Exception Handling
- **Structured Error Responses** with timestamp, status, error code, message, path
- **JWT-Specific Exceptions**:
  - `JwtAuthenticationException` - Invalid JWT token
  - `JwtExpiredException` - Expired JWT token
  - `UnauthorizedException` - Unauthorized access
  - `ForbiddenException` - Forbidden resource
  - `ConflictException` - Resource conflicts (e.g., duplicate email)
- **Comprehensive Exception Handlers**:
  - JWT token validation errors (expired, invalid signature, malformed)
  - Validation errors with field-level details
  - Authentication and authorization errors
  - Missing headers/parameters
  - Type mismatches
  - Method not allowed
  - Resource not found
  - General server errors
- **Proper Logging** using SLF4J with appropriate log levels
- **Enhanced JWT Filter** with proper exception handling and JSON error responses

### 3. API Documentation
All REST endpoints are documented with:
- Operation summaries and descriptions
- Request/response schemas
- HTTP status codes
- Authentication requirements
- Request parameter descriptions
- Example values

## Accessing Swagger UI

Once the application is running, access the Swagger UI at:

```
http://localhost:8080/swagger-ui.html
```

Or access the OpenAPI JSON specification at:

```
http://localhost:8080/v3/api-docs
```

## Using JWT Authentication in Swagger UI

1. **Login to Get JWT Token**:
   - Navigate to the **Authentication** section
   - Execute the `POST /api/auth/login` endpoint
   - Use test credentials or register a new account first
   - Copy the JWT token from the response

2. **Authorize in Swagger UI**:
   - Click the **Authorize** button (lock icon) at the top right
   - Enter your JWT token in the format: `Bearer <your-token>`
   - Click **Authorize**
   - Click **Close**

3. **Make Authenticated Requests**:
   - All endpoints marked with the lock icon now include your JWT token
   - Try endpoints in the **Cart**, **Orders**, sections

## API Endpoints Overview

### Authentication
- `POST /api/auth/register` - Register a new customer
- `POST /api/auth/login` - Login and get JWT token
- `POST /api/auth/logout` - Logout (client-side token removal)

### Products & Categories
- `GET /api/categories` - Get all categories
- `GET /api/products` - Get all products (with filters)
- `GET /api/products/{id}` - Get product by ID
- `GET /api/categories/{id}/products` - Get products by category

### Cart (Requires Authentication)
- `GET /api/cart` - Get current cart
- `POST /api/cart/add` - Add item to cart
- `PUT /api/cart/items/{id}` - Update cart item quantity
- `DELETE /api/cart/items/{id}` - Remove item from cart
- `DELETE /api/cart/clear` - Clear entire cart

### Orders (Requires Authentication)
- `POST /api/orders` - Create new order
- `GET /api/orders/{id}` - Get order by ID
- `GET /api/orders/{id}/status` - Get order status
- `GET /api/orders/my-orders` - Get current user's orders
- `POST /api/orders/{id}/payment-intent` - Create payment intent
- `POST /api/orders/{id}/pay` - Process payment
- `PUT /api/orders/{id}/cancel` - Cancel order

## Error Response Format

All errors follow this standard format:

```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for one or more fields",
  "path": "/api/cart/add",
  "validationErrors": {
    "productId": "Product ID is required",
    "quantity": "Quantity must be at least 1"
  }
}
```

## Exception Handling Best Practices

### 1. Custom Exceptions
- **Domain-specific exceptions** for better error handling
- **HTTP status code annotations** using `@ResponseStatus`
- **Proper exception inheritance** for categorization

### 2. Global Exception Handler
- **Centralized error handling** using `@RestControllerAdvice`
- **Consistent error responses** across all endpoints
- **Detailed logging** for debugging
- **Security-aware messages** (no sensitive data in errors)

### 3. JWT Error Handling
- **Filter-level error handling** for JWT validation
- **Proper HTTP status codes** (401 for authentication errors)
- **Clear error messages** for expired/invalid tokens
- **JSON error responses** even from filters

### 4. Validation Errors
- **Field-level validation** using Jakarta Validation
- **Detailed field errors** in response
- **Human-readable error messages**

## Configuration

### Swagger Configuration (application.properties)

```properties
# Swagger/OpenAPI Configuration
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.enabled=true
springdoc.swagger-ui.tryItOutEnabled=true
springdoc.swagger-ui.operationsSorter=method
springdoc.swagger-ui.tagsSorter=alpha
springdoc.swagger-ui.filter=true
```

### Security Configuration

The following endpoints are publicly accessible:
- Swagger UI: `/swagger-ui/**`, `/swagger-ui.html`
- API Docs: `/v3/api-docs/**`
- Auth endpoints: `/api/auth/**`
- Product browsing: `/api/products/**`, `/api/categories/**`

All other `/api/**` endpoints require JWT authentication.

## Development

### Adding Swagger Annotations to New Endpoints

```java
@Operation(
    summary = "Short description",
    description = "Detailed description of what this endpoint does"
)
@ApiResponses(value = {
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Success message",
        content = @Content(schema = @Schema(implementation = ResponseClass.class))
    ),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "Error message",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
})
@GetMapping("/endpoint")
public ResponseEntity<ApiResponse<ResponseClass>> myEndpoint() {
    // implementation
}
```

### Adding Schema Documentation to DTOs

```java
@Schema(description = "Description of the DTO")
public class MyDto {
    @Schema(description = "Field description", example = "Example value", required = true)
    private String field;
}
```

## Testing

### Manual Testing
1. Start the application
2. Open Swagger UI at `http://localhost:8080/swagger-ui.html`
3. Test authentication flow
4. Test protected endpoints with JWT token

### Automated Testing
- All existing tests should continue to work
- Add integration tests for new exception scenarios
- Test JWT error responses

## Security Notes

1. **JWT Token Expiration**: Tokens expire after 24 hours (configurable in `application.properties`)
2. **HTTPS in Production**: Always use HTTPS in production for API calls
3. **Secure Secrets**: Keep JWT secret key secure and use environment variables in production
4. **CORS Configuration**: Configure CORS properly for production environments
5. **Rate Limiting**: Consider implementing rate limiting for public endpoints

## Troubleshooting

### Swagger UI Not Loading
- Check that the application is running on the correct port
- Verify security configuration allows access to Swagger endpoints
- Clear browser cache and reload

### JWT Authentication Not Working
- Ensure token format is `Bearer <token>` (with space after "Bearer")
- Check token expiration time
- Verify JWT secret key matches between token generation and validation

### 401 Errors on Protected Endpoints
- Ensure you've clicked "Authorize" in Swagger UI
- Verify the token is still valid (not expired)
- Check that the token was obtained from a successful login

## Future Enhancements

- API versioning support
- Rate limiting documentation
- WebSocket endpoint documentation
- Request/response examples from actual API calls
- API changelog tracking
