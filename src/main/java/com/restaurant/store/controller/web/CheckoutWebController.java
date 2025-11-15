package com.restaurant.store.controller.web;

import com.restaurant.store.dto.response.CartResponse;
import com.restaurant.store.entity.Customer;
import com.restaurant.store.exception.ResourceNotFoundException;
import com.restaurant.store.exception.UnauthorizedException;
import com.restaurant.store.security.AuthHelper;
import com.restaurant.store.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    public String checkout(Model model, RedirectAttributes redirectAttributes) {
        try {
            Customer customer = authHelper.user();
            CartResponse cart = cartService.getCartByCustomerId(customer.getId());

            model.addAttribute("cart", cart);
            model.addAttribute("stripePublicKey", stripePublicKey);

            return "checkout";
        } catch (UnauthorizedException ex) {
            log.debug("Unauthorized checkout access: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Please login to continue to checkout");
            return "redirect:/login";
        } catch (ResourceNotFoundException ex) {
            log.warn("Cart data missing for checkout: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/cart";
        }
    }
}
