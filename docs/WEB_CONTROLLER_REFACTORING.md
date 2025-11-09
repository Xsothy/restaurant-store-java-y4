# Web Controller Refactoring

## Overview

This document describes the refactoring of web controllers to improve code organization and simplify authentication handling.

## Changes Made

### 1. Authentication Helper (AuthHelper)

Created a new `AuthHelper` utility class similar to Laravel's `auth()` helper that provides convenient access to the currently authenticated user.

**Location**: `com.restaurant.store.security.AuthHelper`

#### Features

The `AuthHelper` provides the following methods:

```java
// Get current authenticated customer (throws exception if not authenticated)
Customer customer = authHelper.user();

// Get current authenticated customer as Optional
Optional<Customer> customer = authHelper.userOrNull();

// Get current user ID
Long customerId = authHelper.id();

// Check if user is authenticated
boolean isAuthenticated = authHelper.check();

// Check if user is guest (not authenticated)
boolean isGuest = authHelper.guest();

// Get current user email
String email = authHelper.email();
```

#### Usage Example

**Before (Complex):**
```java
@GetMapping("/profile")
public String profile(Model model, @CookieValue(value = "jwt", required = false) String jwtCookie) {
    try {
        String token = getAuthToken(jwtCookie);
        Customer customer = getCustomerFromAuth();
        // ... use customer
    } catch (Exception e) {
        return "redirect:/login";
    }
}

private String getAuthToken(String jwtCookie) {
    if (jwtCookie != null && !jwtCookie.isEmpty()) {
        return "Bearer " + jwtCookie;
    }
    throw new UnauthorizedException("Authentication required");
}

private Customer getCustomerFromAuth() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.isAuthenticated()) {
        String email = authentication.getName();
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Customer not found"));
    }
    throw new UnauthorizedException("Authentication required");
}
```

**After (Simple):**
```java
@GetMapping("/profile")
public String profile(Model model) {
    try {
        // Get authenticated customer using AuthHelper
        Customer customer = authHelper.user();
        // ... use customer
    } catch (Exception e) {
        return "redirect:/login";
    }
}
```

### 2. Web Controller Reorganization

Split the monolithic `WebController` into specialized controllers for better organization and maintainability.

#### Before

All routes were handled in a single `WebController`:
- Login/Register pages
- Cart page
- Checkout page
- Profile page
- Payment success/cancel pages

#### After

Organized into dedicated controllers:

1. **HomeWebController** - Root level routes
   - `GET /` - Redirect to menu
   - `GET /login` - Login page
   - `GET /register` - Registration page

2. **AuthWebController** - Authentication
   - `GET /auth/login` - Login page
   - `POST /auth/login` - Process login
   - `GET /auth/register` - Registration page
   - `POST /auth/register` - Process registration
   - `POST /auth/logout` - Logout

3. **CartWebController** - Shopping cart
   - `GET /cart` - Cart page

4. **CheckoutWebController** - Checkout
   - `GET /checkout` - Checkout page

5. **ProfileWebController** - Customer profile
   - `GET /profile` - Profile page

6. **OrderWebController** - Orders
   - `GET /orders` - Orders list
   - `GET /orders/{orderId}` - Order details

7. **PaymentWebController** - Payment results
   - `GET /payment/success` - Payment success page
   - `GET /payment/cancel` - Payment cancel page

8. **MenuController** - Product browsing (already existed)
   - `GET /menu` - Menu page
   - `GET /products/{id}` - Product details

9. **WebController** - Miscellaneous (minimal)
   - Just maintains redirect from `/` to `/menu`

### 3. Service Layer Enhancement

Added `getCartByCustomerId()` method to `CartService` for internal use by web controllers:

```java
/**
 * Get cart by customer ID (for internal use, e.g., web controllers).
 */
public CartResponse getCartByCustomerId(Long customerId) {
    Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    Cart cart = cartRepository.findByCustomerId(customer.getId())
            .orElseGet(() -> createNewCart(customer));
    
    return getCartResponse(cart.getId());
}
```

## Benefits

### 1. Simplified Authentication

- **Single line of code** to get authenticated user: `Customer customer = authHelper.user();`
- **No boilerplate** for token extraction or authentication context handling
- **Consistent approach** across all controllers
- **Type-safe** - returns actual `Customer` entity, not just email or ID

### 2. Better Code Organization

- **Single Responsibility** - Each controller handles one concern
- **Easy to locate** - Finding auth code? Check `AuthWebController`
- **Reduced file size** - Controllers are smaller and easier to understand
- **Clear naming** - Controller names indicate their purpose

### 3. Improved Maintainability

- **Easier to test** - Smaller controllers with focused responsibilities
- **Easier to modify** - Changes to one feature don't affect others
- **Easier to understand** - New developers can quickly grasp the structure
- **Easier to extend** - Adding new features doesn't clutter existing controllers

### 4. Enhanced Security

- **Authorization checks** - Easy to verify user owns resources
- **Example in OrderWebController**:
  ```java
  // Verify order belongs to customer
  if (!order.getCustomerId().equals(customer.getId())) {
      log.warn("Customer {} attempted to access order {} belonging to another customer", 
              customer.getId(), orderId);
      return "redirect:/orders";
  }
  ```

## Controller Structure

### Recommended Pattern

All web controllers should follow this pattern:

```java
package com.restaurant.store.controller.web;

import com.restaurant.store.entity.Customer;
import com.restaurant.store.security.AuthHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for [feature] pages.
 */
@Controller
@RequestMapping("/base-path")
@RequiredArgsConstructor
@Slf4j
public class FeatureWebController {

    private final AuthHelper authHelper;
    private final FeatureService featureService;

    /**
     * Display [feature] page.
     */
    @GetMapping
    public String featurePage(Model model) {
        try {
            // Get authenticated customer
            Customer customer = authHelper.user();
            
            // Business logic
            // ...
            
            // Add attributes to model
            model.addAttribute("data", data);
            
            return "template-name";
        } catch (Exception e) {
            log.warn("Failed to load [feature]: {}", e.getMessage());
            return "redirect:/login";
        }
    }
}
```

### Key Elements

1. **Package**: `com.restaurant.store.controller.web`
2. **Annotations**: `@Controller`, `@RequestMapping`, `@RequiredArgsConstructor`, `@Slf4j`
3. **Dependencies**: Inject via constructor (using `@RequiredArgsConstructor`)
4. **Authentication**: Use `authHelper.user()` to get current customer
5. **Error Handling**: Catch exceptions and redirect to appropriate page
6. **Logging**: Use `@Slf4j` for logging warnings and errors
7. **Documentation**: Add JavaDoc comments for public methods

## Migration Guide

### For Developers

When creating new web controllers:

1. ✅ **Use AuthHelper** instead of manual authentication
2. ✅ **Create focused controllers** - one per feature/concern
3. ✅ **Follow naming convention** - `[Feature]WebController`
4. ✅ **Add documentation** - JavaDoc comments on class and methods
5. ✅ **Use constructor injection** - with `@RequiredArgsConstructor`
6. ✅ **Handle errors gracefully** - redirect to appropriate pages
7. ✅ **Log warnings** - use `@Slf4j` for debugging

### Existing Code

If you find old code using manual authentication:

**Replace this:**
```java
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
if (authentication != null && authentication.isAuthenticated()) {
    String email = authentication.getName();
    Customer customer = customerRepository.findByEmail(email)
            .orElseThrow(() -> new UnauthorizedException("Customer not found"));
}
```

**With this:**
```java
Customer customer = authHelper.user();
```

## Testing

### Unit Testing Controllers

```java
@WebMvcTest(ProfileWebController.class)
class ProfileWebControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private AuthHelper authHelper;
    
    @MockBean
    private OrderService orderService;
    
    @Test
    void shouldDisplayProfile() throws Exception {
        // Given
        Customer mockCustomer = new Customer();
        mockCustomer.setId(1L);
        mockCustomer.setName("Test User");
        
        when(authHelper.user()).thenReturn(mockCustomer);
        when(orderService.getCustomerOrders(1L, null)).thenReturn(List.of());
        
        // When/Then
        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attributeExists("customer"));
    }
}
```

## Future Improvements

1. **Role-Based Access Control**
   - Add methods to AuthHelper: `hasRole()`, `hasPermission()`
   - Example: `authHelper.hasRole("ADMIN")`

2. **Method-Level Security**
   - Use `@PreAuthorize` with AuthHelper
   - Example: `@PreAuthorize("@authHelper.check()")`

3. **Request-Scoped Caching**
   - Cache authenticated user per request
   - Avoid multiple database queries

4. **Custom Annotations**
   - Create `@RequireAuth` annotation
   - Automatically inject authenticated user

## Summary

The web controller refactoring provides:

- ✅ **Simplified authentication** - Laravel-style `auth()->user()` equivalent
- ✅ **Better organization** - Dedicated controllers per feature
- ✅ **Improved maintainability** - Smaller, focused, well-documented code
- ✅ **Enhanced security** - Easier to implement authorization checks
- ✅ **Consistent patterns** - All controllers follow same structure

## References

- [AuthHelper.java](../src/main/java/com/restaurant/store/security/AuthHelper.java)
- [ProfileWebController.java](../src/main/java/com/restaurant/store/controller/web/ProfileWebController.java)
- [CartWebController.java](../src/main/java/com/restaurant/store/controller/web/CartWebController.java)
- [CheckoutWebController.java](../src/main/java/com/restaurant/store/controller/web/CheckoutWebController.java)
- [OrderWebController.java](../src/main/java/com/restaurant/store/controller/web/OrderWebController.java)
- [PaymentWebController.java](../src/main/java/com/restaurant/store/controller/web/PaymentWebController.java)
- [HomeWebController.java](../src/main/java/com/restaurant/store/controller/web/HomeWebController.java)
