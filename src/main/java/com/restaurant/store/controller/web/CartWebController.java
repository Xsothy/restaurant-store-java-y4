package com.restaurant.store.controller.web;

import com.restaurant.store.dto.response.CartResponse;
import com.restaurant.store.entity.Customer;
import com.restaurant.store.security.AuthHelper;
import com.restaurant.store.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for shopping cart pages.
 */
@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
@Slf4j
public class CartWebController {

    private final AuthHelper authHelper;
    private final CartService cartService;

    /**
     * Display shopping cart page.
     */
    @GetMapping
    public String cart(Model model) {
        try {
            // Get authenticated customer using AuthHelper
            Customer customer = authHelper.user();
            
            // Get customer's cart
            CartResponse cart = cartService.getCartByCustomerId(customer.getId());
            model.addAttribute("cart", cart);
            
            return "cart";
        } catch (Exception e) {
            log.warn("Failed to load cart: {}", e.getMessage());
            model.addAttribute("cart", null);
            return "cart";
        }
    }
}
