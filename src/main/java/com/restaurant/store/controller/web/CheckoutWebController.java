package com.restaurant.store.controller.web;

import com.restaurant.store.dto.response.CartResponse;
import com.restaurant.store.entity.Customer;
import com.restaurant.store.security.AuthHelper;
import com.restaurant.store.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for checkout pages.
 */
@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
@Slf4j
public class CheckoutWebController {

    private final AuthHelper authHelper;
    private final CartService cartService;
    
    @Value("${stripe.api.key}")
    private String stripePublicKey;

    /**
     * Display checkout page with cart and Stripe configuration.
     */
    @GetMapping
    public String checkout(Model model) {
        try {
            // Get authenticated customer using AuthHelper
            Customer customer = authHelper.user();
            
            // Get customer's cart
            CartResponse cart = cartService.getCartByCustomerId(customer.getId());
            
            // Add attributes to model
            model.addAttribute("cart", cart);
            model.addAttribute("stripePublicKey", stripePublicKey);
            
            return "checkout";
        } catch (Exception e) {
            log.warn("Failed to load cart for checkout: {}", e.getMessage());
            return "redirect:/cart";
        }
    }
}
