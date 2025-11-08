# Web Template Architecture

## Overview

This document describes the architecture for web templates in the Restaurant Store application. The key principle is: **Never use API calls directly in web templates**. All data should be fetched server-side and passed to templates via the Model.

## Authentication Flow

### JWT Storage
- JWT tokens are stored in **both** localStorage and HTTP cookies
- **localStorage**: Used for client-side JavaScript (e.g., dynamic cart updates)
- **Cookie**: Used for server-side rendering in WebController

### Login/Register Flow
When a user logs in or registers:
1. API returns JWT token in response
2. Frontend saves token to `localStorage.setItem('token', token)`
3. Frontend also sets cookie: `document.cookie = 'jwt=${token}; path=/; max-age=86400; SameSite=Lax'`
4. Customer data saved to `localStorage.setItem('customer', JSON.stringify(customer))`

### Logout Flow
When a user logs out:
1. Clear localStorage: `localStorage.removeItem('token')` and `localStorage.removeItem('customer')`
2. Clear cookie: `document.cookie = 'jwt=; path=/; max-age=0'`
3. Redirect to login page

## WebController Pattern

### Server-Side Data Fetching
The `WebController` fetches data server-side and passes it to templates via Spring Model:

```java
@GetMapping("/orders")
public String orders(Model model, @CookieValue(value = "jwt", required = false) String jwtCookie) {
    try {
        String token = getAuthToken(jwtCookie);
        List<OrderResponse> customerOrders = orderService.getMyOrders(token);
        model.addAttribute("orders", customerOrders);
    } catch (Exception e) {
        log.warn("Failed to load orders: {}", e.getMessage());
        model.addAttribute("orders", List.of());
    }
    return "orders";
}
```

### Helper Methods
- `getAuthToken(String jwtCookie)`: Converts cookie JWT to "Bearer {token}" format for services
- `getCustomerFromAuth()`: Gets authenticated customer from Spring Security context

## Template Data Injection

### Using Thymeleaf Inline JavaScript
Templates receive data from server and inject it into JavaScript:

```html
<script th:inline="javascript">
    /*<![CDATA[*/
    const ordersData = /*[[${orders}]]*/ [];
    
    function ordersApp() {
        return {
            orders: ordersData,
            loading: false,
            
            init() {
                // Data already loaded from server
                console.log('Orders loaded:', this.orders.length);
            }
        }
    }
    /*]]>*/
</script>
```

### Current Implementation Status

#### ✅ Implemented (Server-Side Rendering)
- **Menu** (`/menu`): Categories and products passed from server
- **Product Details** (`/products/{id}`): Product data passed from server
- **Cart** (`/cart`): Cart data passed from server (WebController enhanced)
- **Checkout** (`/checkout`): Cart data and Stripe key passed from server
- **Orders** (`/orders`): Orders list passed from server
- **Profile** (`/profile`): Customer data and stats passed from server

#### ⚠️ Needs Refactoring
These templates currently use `fetch()` API calls and need to be updated to use server-provided data:
- `cart.html` - Still makes API calls for cart operations (add/update/remove)
- `orders.html` - Still makes API call to load orders
- `profile.html` - Still makes API calls for profile data and stats
- `checkout.html` - Still makes API calls for cart data and order creation

## Migration Guide

### Step 1: Update WebController
Add endpoint that fetches data server-side:

```java
@GetMapping("/your-page")
public String yourPage(Model model, @CookieValue(value = "jwt", required = false) String jwtCookie) {
    try {
        String token = getAuthToken(jwtCookie);
        YourData data = yourService.getData(token);
        model.addAttribute("data", data);
    } catch (Exception e) {
        log.warn("Failed to load data: {}", e.getMessage());
        model.addAttribute("data", null);
    }
    return "your-page";
}
```

### Step 2: Update Template
Replace API call with server-provided data:

**Before (API Call):**
```javascript
async init() {
    const token = localStorage.getItem('token');
    const response = await fetch(`${API_BASE_URL}/endpoint`, {
        headers: { 'Authorization': `Bearer ${token}` }
    });
    const data = await response.json();
    this.data = data.data;
}
```

**After (Server-Side Data):**
```html
<script th:inline="javascript">
    /*<![CDATA[*/
    const serverData = /*[[${data}]]*/ null;
    
    function yourApp() {
        return {
            data: serverData,
            
            init() {
                // Data already loaded from server
                console.log('Data loaded:', this.data);
            }
        }
    }
    /*]]>*/
</script>
```

## Dynamic Operations

For operations that modify data (add to cart, update quantity, etc.), you can:

### Option 1: Form Submissions with Redirects
Use traditional form POST with Spring MVC:

```java
@PostMapping("/cart/add")
public String addToCart(@RequestParam Long productId, @RequestParam int quantity) {
    // Add to cart logic
    return "redirect:/cart";
}
```

### Option 2: HTMX (Recommended for SPA-like Experience)
Use HTMX for partial page updates without full refresh.

### Option 3: Keep API Calls for Dynamic Operations
For highly interactive operations, keep using fetch() with JWT from localStorage, but still load initial data from server.

## Security Considerations

1. **Cookie Security**: Use `SameSite=Lax` to prevent CSRF attacks
2. **Cookie Expiry**: Set appropriate `max-age` (currently 24 hours: 86400 seconds)
3. **HTTPS Only**: In production, add `Secure` flag to cookies
4. **XSS Protection**: Always sanitize data before rendering in templates

## Benefits of This Architecture

1. **SEO Friendly**: Pages render with data on first load
2. **Faster Initial Load**: No waiting for API calls to complete
3. **Better UX**: No loading spinners for initial data
4. **Simpler Code**: No complex state management for initial load
5. **Security**: Server validates authentication before rendering sensitive data

## Next Steps

1. Refactor remaining templates to use server-side data
2. Add HTMX for dynamic interactions
3. Implement proper error pages for authentication failures
4. Add loading states only for user-initiated actions (not initial load)
