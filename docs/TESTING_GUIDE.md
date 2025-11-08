# Testing Guide

## Overview

This document provides guidance on running and maintaining integration tests for the Restaurant Store API's open (public) APIs.

## Test Structure

Integration tests are located in `src/test/java/com/restaurant/store/controller/api/` and cover all open API endpoints:

- **AuthControllerIntegrationTest** - Authentication APIs (register, login, logout)
- **ProductControllerIntegrationTest** - Product and category browsing APIs
- **CartControllerIntegrationTest** - Shopping cart management APIs  
- **OrderControllerIntegrationTest** - Order management APIs

## Test Configuration

### Test Profile

Tests use a separate `test` profile with configuration in `src/test/resources/application-test.properties`:

- **Database**: H2 in-memory database (no PostgreSQL required)
- **SQL Initialization**: Disabled (tables created via JPA DDL)
- **Admin API Sync**: Disabled
- **Payment Service**: Set to `intent` mode with test Stripe keys

### Dependencies

Test dependencies are configured in `pom.xml`:
- `spring-boot-starter-test` - Spring Boot testing support
- `spring-security-test` - Security testing utilities
- `h2` - In-memory database for tests

## Running Tests

### Run All API Tests

```bash
mvn test -Dtest="*ControllerIntegrationTest"
```

### Run Specific Test Class

```bash
mvn test -Dtest=AuthControllerIntegrationTest
mvn test -Dtest=ProductControllerIntegrationTest
mvn test -Dtest=CartControllerIntegrationTest
mvn test -Dtest=OrderControllerIntegrationTest
```

### Run Specific Test Method

```bash
mvn test -Dtest=AuthControllerIntegrationTest#testLogin_Success
```

### Run All Tests

```bash
mvn test
```

## Test Coverage

### AuthController (8 tests)
- ✅ Register new customer  
- ✅ Register with existing email
- ✅ Register with invalid data
- ✅ Login with valid credentials
- ✅ Login with invalid credentials
- ✅ Login with non-existent email
- ✅ Logout with valid token
- ✅ Logout without token

### ProductController (10 tests)
- ✅ Get all categories
- ✅ Get all products
- ✅ Get only available products
- ✅ Get products by category
- ✅ Get available products by category
- ✅ Get product by ID
- ✅ Get product by ID (404 Not Found)
- ✅ Get products by category endpoint
- ✅ Get products for non-existent category (404 Not Found)
- ✅ Get products for empty category

### CartController (11 tests)
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

### OrderController (11 tests)
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

## Test Patterns

### Authentication Flow

Most tests that require authentication follow this pattern:

```java
// 1. Create a customer
Customer customer = new Customer();
customer.setEmail("test@example.com");
customer.setPasswordHash(passwordEncoder.encode("password123"));
// ... set other fields
customerRepository.save(customer);

// 2. Login to get token
LoginRequest loginRequest = new LoginRequest();
loginRequest.setEmail("test@example.com");
loginRequest.setPassword("password123");

String token = // Extract from login response

// 3. Use token in requests
mockMvc.perform(get("/api/cart")
        .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
```

### Data Setup

Tests use `@BeforeEach` to set up test data:

```java
@BeforeEach
void setUp() {
    // Clean up previous test data
    repository.deleteAll();
    
    // Create test data
    // ...
}
```

### Transactional Tests

All tests use `@Transactional` to ensure database state is rolled back after each test.

## Common Issues and Solutions

### Issue: Application Context Fails to Load

**Symptom**: Tests fail with "ApplicationContext failure threshold exceeded"

**Solution**: Check `application-test.properties` configuration. Ensure:
- Database URL is correct
- SQL initialization is disabled (`spring.sql.init.mode=never`)
- Required properties are set

### Issue: 302 Redirects Instead of 401

**Symptom**: Tests expecting 401 Unauthorized get 302 Redirect

**Cause**: Spring Security form login redirects unauthorized requests

**Solution**: Tests should handle both status codes or use `is4xxClientError()`

### Issue: Field Name Mismatch

**Symptom**: JSON path assertions fail (e.g., `$.data.available` vs `$.data.isAvailable`)

**Solution**: Check the actual DTO response class to verify field names

### Issue: Payment Tests Fail

**Symptom**: Payment Intent creation fails in tests

**Cause**: Stripe API calls require valid credentials or mocking

**Solution**: 
- Mock Stripe service for unit tests
- Use test Stripe keys for integration tests
- Or skip payment tests if Stripe is not configured

## Writing New Tests

When adding new API endpoints, follow these guidelines:

### 1. Test Coverage

Ensure tests cover:
- ✅ Success scenarios (200, 201 responses)
- ✅ Validation failures (400 Bad Request)
- ✅ Authentication failures (401 Unauthorized)
- ✅ Resource not found (404 Not Found)
- ✅ Business logic errors (409 Conflict, etc.)

### 2. Test Naming

Use descriptive names that explain the scenario:
```java
@Test
@DisplayName("Should return 404 when product not found")
void testGetProductById_NotFound() {
    // ...
}
```

### 3. Assertions

Use appropriate assertions:
```java
// Status assertions
.andExpect(status().isOk())
.andExpect(status().isCreated())
.andExpect(status().isBadRequest())
.andExpect(status().isNotFound())

// JSON path assertions
.andExpect(jsonPath("$.success").value(true))
.andExpect(jsonPath("$.data.name").value("Product Name"))
.andExpect(jsonPath("$.data.items", hasSize(2)))
```

### 4. Test Independence

Ensure tests are independent:
- Don't rely on execution order
- Clean up data in `@BeforeEach`
- Use unique test data for each test

## Continuous Integration

Tests should be run as part of CI/CD pipeline:

```yaml
# Example GitHub Actions workflow
- name: Run Tests
  run: mvn test

- name: Generate Test Report
  if: always()
  uses: dorny/test-reporter@v1
  with:
    name: Test Results
    path: target/surefire-reports/*.xml
    reporter: java-junit
```

## Test Maintenance

### Regular Updates

- Update tests when API contracts change
- Add tests for new endpoints
- Remove tests for deprecated endpoints
- Keep test data realistic and up-to-date

### Code Review Checklist

- [ ] All new endpoints have corresponding tests
- [ ] Tests cover success and error scenarios  
- [ ] Test names are descriptive
- [ ] No hardcoded credentials or sensitive data
- [ ] Tests are independent and can run in any order
- [ ] Mocking is used appropriately

## Debugging Tests

### Enable Debug Logging

Add to `application-test.properties`:
```properties
logging.level.com.restaurant.store=DEBUG
logging.level.org.springframework.web=DEBUG
```

### Run Single Test with Details

```bash
mvn test -Dtest=TestClass#testMethod -X
```

### Check Test Reports

Test reports are generated in `target/surefire-reports/`:
- `*.txt` - Text format test results
- `*.xml` - JUnit XML format for CI tools

## Performance Considerations

- H2 in-memory database is fast but limited
- Consider test execution time when adding complex tests
- Use `@DirtiesContext` sparingly as it slows down tests
- Group related tests in same class to share context

## References

- [Spring Boot Testing Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [Spring MVC Test Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/testing.html#spring-mvc-test-framework)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
