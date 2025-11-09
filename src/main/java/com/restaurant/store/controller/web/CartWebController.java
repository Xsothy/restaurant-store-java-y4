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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
            return "redirect:/login";
        }
    }

    @PostMapping("/items/{cartItemId}/quantity")
    public String updateQuantity(@PathVariable Long cartItemId,
                                 @RequestParam String action,
                                 RedirectAttributes redirectAttributes) {
        try {
            Customer customer = authHelper.user();

            if ("increase".equalsIgnoreCase(action)) {
                cartService.incrementCartItemForCustomer(cartItemId, customer.getId());
            } else if ("decrease".equalsIgnoreCase(action)) {
                cartService.decrementCartItemForCustomer(cartItemId, customer.getId());
            } else {
                redirectAttributes.addFlashAttribute("cartError", "Unsupported cart action");
            }
        } catch (Exception e) {
            log.warn("Unable to update cart item {}: {}", cartItemId, e.getMessage());
            redirectAttributes.addFlashAttribute("cartError", e.getMessage());
        }

        return "redirect:/cart";
    }

    @PostMapping("/items/{cartItemId}/remove")
    public String removeItem(@PathVariable Long cartItemId, RedirectAttributes redirectAttributes) {
        try {
            Customer customer = authHelper.user();
            cartService.removeCartItemForCustomer(cartItemId, customer.getId());
            redirectAttributes.addFlashAttribute("cartMessage", "Item removed from cart");
        } catch (Exception e) {
            log.warn("Unable to remove cart item {}: {}", cartItemId, e.getMessage());
            redirectAttributes.addFlashAttribute("cartError", e.getMessage());
        }

        return "redirect:/cart";
    }
}
