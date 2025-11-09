package com.restaurant.store.controller.web;

import com.restaurant.store.dto.response.OrderResponse;
import com.restaurant.store.entity.Customer;
import com.restaurant.store.security.AuthHelper;
import com.restaurant.store.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
    public String orders(Model model) {
        try {
            // Get authenticated customer using AuthHelper
            Customer customer = authHelper.user();
            
            // Get customer orders
            List<OrderResponse> orders = orderService.getCustomerOrders(customer.getId(), null);
            model.addAttribute("orders", orders);
            
            return "orders";
        } catch (Exception e) {
            log.warn("Failed to load orders: {}", e.getMessage());
            return "redirect:/auth/login";
        }
    }
    
    /**
     * Display order details page.
     */
    @GetMapping("/{orderId}")
    public String orderDetails(@PathVariable Long orderId, Model model) {
        try {
            // Get authenticated customer using AuthHelper
            Customer customer = authHelper.user();
            
            // Get order details
            OrderResponse order = orderService.getOrderById(orderId, null);
            
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
            return "redirect:/auth/login";
        }
    }
}