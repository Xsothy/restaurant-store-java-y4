package com.restaurant.store.controller.web;

import com.restaurant.store.dto.response.OrderResponse;
import com.restaurant.store.entity.Customer;
import com.restaurant.store.exception.BadRequestException;
import com.restaurant.store.exception.ResourceNotFoundException;
import com.restaurant.store.exception.UnauthorizedException;
import com.restaurant.store.security.AuthHelper;
import com.restaurant.store.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller for order-related web pages.
 */
@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderWebController {
    
    private final AuthHelper authHelper;
    private final OrderService orderService;
    
    /**
     * Display orders list page.
     */
    @GetMapping
    public String orders(Model model, RedirectAttributes redirectAttributes) {
        try {
            Customer customer = authHelper.user();
            List<OrderResponse> orders = orderService.getOrdersForCustomer(customer.getId());
            model.addAttribute("orders", orders);

            return "orders";
        } catch (UnauthorizedException ex) {
            log.debug("Unauthorized orders access: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Please login to view your orders");
            return "redirect:/login";
        }
    }
    
    /**
     * Display order details page.
     */
    @GetMapping("/{orderId}")
    public String orderDetails(@PathVariable Long orderId, Model model, RedirectAttributes redirectAttributes) {
        try {
            Customer customer = authHelper.user();
            OrderResponse order = orderService.getOrderForCustomer(orderId, customer.getId());

            if (!order.getCustomerId().equals(customer.getId())) {
                log.warn("Customer {} attempted to access order {} belonging to another customer",
                        customer.getId(), orderId);
                redirectAttributes.addFlashAttribute("errorMessage", "You cannot view this order");
                return "redirect:/orders";
            }

            model.addAttribute("order", order);
            return "order-details";
        } catch (UnauthorizedException ex) {
            log.debug("Unauthorized order details access: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Please login to view your orders");
            return "redirect:/login";
        } catch (BadRequestException | ResourceNotFoundException ex) {
            log.warn("Unable to load order {} for details view: {}", orderId, ex.getMessage());
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("order", null);
            return "order-details";
        }
    }

    @PostMapping("/{orderId}/cancel")
    public String cancelOrder(@PathVariable Long orderId, RedirectAttributes redirectAttributes) {
        try {
            Customer customer = authHelper.user();
            orderService.cancelOrderForCustomer(orderId, customer.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Order cancelled successfully");
            return "redirect:/orders/" + orderId;
        } catch (UnauthorizedException ex) {
            log.debug("Unauthorized order cancellation attempt: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Please login to manage your orders");
            return "redirect:/login";
        } catch (BadRequestException | ResourceNotFoundException ex) {
            log.warn("Failed to cancel order {}: {}", orderId, ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/orders/" + orderId;
        }
    }
}