package com.restaurant.store.controller;

import com.restaurant.store.dto.response.CartResponse;
import com.restaurant.store.dto.response.CategoryResponse;
import com.restaurant.store.dto.response.OrderResponse;
import com.restaurant.store.dto.response.ProductResponse;
import com.restaurant.store.entity.Customer;
import com.restaurant.store.exception.UnauthorizedException;
import com.restaurant.store.repository.CustomerRepository;
import com.restaurant.store.service.CartService;
import com.restaurant.store.service.OrderService;
import com.restaurant.store.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebController {
    
    private final ProductService productService;
    private final CartService cartService;
    private final OrderService orderService;
    private final CustomerRepository customerRepository;
    
    @Value("${stripe.api.key}")
    private String stripePublicKey;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping({"/", "/menu"})
    public String menu(Model model) {
        List<CategoryResponse> categories = productService.getAllCategories();
        List<ProductResponse> products = productService.getAllProducts(null, true);
        
        model.addAttribute("categories", categories);
        model.addAttribute("products", products);
        return "menu";
    }

    @GetMapping("/products/{productId}")
    public String productDetails(@PathVariable Long productId, Model model) {
        ProductResponse product = productService.getProductById(productId);
        model.addAttribute("product", product);
        return "product-details";
    }

    @GetMapping("/cart")
    public String cart(Model model, @CookieValue(value = "jwt", required = false) String jwtCookie) {
        try {
            String token = getAuthToken(jwtCookie);
            CartResponse cart = cartService.getCart(token);
            model.addAttribute("cart", cart);
        } catch (Exception e) {
            log.warn("Failed to load cart: {}", e.getMessage());
            // Pass empty cart data
            model.addAttribute("cart", null);
        }
        return "cart";
    }
    
    @GetMapping("/checkout")
    public String checkout(Model model, @CookieValue(value = "jwt", required = false) String jwtCookie) {
        try {
            String token = getAuthToken(jwtCookie);
            CartResponse cart = cartService.getCart(token);
            model.addAttribute("cart", cart);
            model.addAttribute("stripePublicKey", stripePublicKey);
        } catch (Exception e) {
            log.warn("Failed to load cart for checkout: {}", e.getMessage());
            return "redirect:/cart";
        }
        return "checkout";
    }

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

    @GetMapping("/profile")
    public String profile(Model model, @CookieValue(value = "jwt", required = false) String jwtCookie) {
        try {
            String token = getAuthToken(jwtCookie);
            Customer customer = getCustomerFromAuth();
            List<OrderResponse> customerOrders = orderService.getMyOrders(token);
            
            // Calculate stats
            int totalOrders = customerOrders.size();
            BigDecimal totalSpent = customerOrders.stream()
                    .map(OrderResponse::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            model.addAttribute("customer", customer);
            model.addAttribute("totalOrders", totalOrders);
            model.addAttribute("totalSpent", totalSpent);
        } catch (Exception e) {
            log.warn("Failed to load profile: {}", e.getMessage());
            return "redirect:/login";
        }
        return "profile";
    }
    
    @GetMapping("/payment/success")
    public String paymentSuccess(@RequestParam(required = false) String session_id, Model model) {
        model.addAttribute("sessionId", session_id);
        return "payment-success";
    }
    
    @GetMapping("/payment/cancel")
    public String paymentCancel(@RequestParam(required = false) String session_id, Model model) {
        model.addAttribute("sessionId", session_id);
        return "payment-cancel";
    }
    
    /**
     * Get JWT token from cookie or create Bearer token format for services
     */
    private String getAuthToken(String jwtCookie) {
        if (jwtCookie != null && !jwtCookie.isEmpty()) {
            return "Bearer " + jwtCookie;
        }
        throw new UnauthorizedException("Authentication required");
    }
    
    /**
     * Get authenticated customer from Spring Security context
     */
    private Customer getCustomerFromAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            return customerRepository.findByEmail(email)
                    .orElseThrow(() -> new UnauthorizedException("Customer not found"));
        }
        throw new UnauthorizedException("Authentication required");
    }
}
