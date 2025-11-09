package com.restaurant.store.controller.web;

import com.restaurant.store.dto.response.OrderResponse;
import com.restaurant.store.entity.Customer;
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
            // Get authenticated customer using AuthHelper
            Customer customer = authHelper.user();

            // Get customer orders
            List<OrderResponse> orders = orderService.getOrdersForCustomer(customer.getId());
            model.addAttribute("orders", orders);

            return "orders";
        } catch (Exception e) {
            log.warn("Failed to load orders: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        }
    }
    
    /**
     * Display order details page.
     */
    @GetMapping("/{orderId}")
    public String orderDetails(@PathVariable Long orderId, Model model, RedirectAttributes redirectAttributes) {
        try {
            // Get authenticated customer using AuthHelper
            Customer customer = authHelper.user();

            // Get order details
            OrderResponse order = orderService.getOrderForCustomer(orderId, customer.getId());

            // Verify order belongs to customer
            if (!order.getCustomerId().equals(customer.getId())) {
                log.warn("Customer {} attempted to access order {} belonging to another customer",
                        customer.getId(), orderId);
                return "redirect:/orders";
            }

            model.addAttribute("order", order);
            return "order-details";
        } catch (Exception e) {
            log.warn("Failed to load order details: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/login";
        }
    }

    @PostMapping("/{orderId}/cancel")
    public String cancelOrder(@PathVariable Long orderId, RedirectAttributes redirectAttributes) {
        try {
            Customer customer = authHelper.user();
            orderService.cancelOrderForCustomer(orderId, customer.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Order cancelled successfully");
            return "redirect:/orders/" + orderId;
        } catch (Exception e) {
            log.warn("Failed to cancel order {}: {}", orderId, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/orders/" + orderId;
        }
    }
}