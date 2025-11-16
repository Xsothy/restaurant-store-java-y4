package com.restaurant.store.controller.web;

import com.restaurant.store.dto.request.CreateOrderRequest;
import com.restaurant.store.dto.response.ApiResponse;
import com.restaurant.store.dto.response.CartResponse;
import com.restaurant.store.dto.response.OrderResponse;
import com.restaurant.store.entity.Customer;
import com.restaurant.store.exception.BadRequestException;
import com.restaurant.store.exception.ResourceNotFoundException;
import com.restaurant.store.exception.UnauthorizedException;
import com.restaurant.store.security.AuthHelper;
import com.restaurant.store.service.CartService;
import com.restaurant.store.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

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
    private final OrderService orderService;
    
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

    @PostMapping("/place-order")
    @ResponseBody
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        try {
            Customer customer = authHelper.user();
            OrderResponse order = orderService.createOrderForCustomer(request, customer.getId());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Order created successfully", order));
        } catch (UnauthorizedException ex) {
            log.debug("Unauthorized order creation attempt: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Please login to place an order"));
        } catch (BadRequestException | ResourceNotFoundException ex) {
            log.warn("Failed to create order: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(ex.getMessage()));
        }
    }
}
