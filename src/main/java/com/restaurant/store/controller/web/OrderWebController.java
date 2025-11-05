package com.restaurant.store.controller.web;

import com.restaurant.store.dto.response.OrderResponse;
import com.restaurant.store.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/orders")
public class OrderWebController {
    
    @Autowired
    private OrderService orderService;
    
    @GetMapping
    public String orders(Model model) {
        // TODO: Get current customer from session
        Long customerId = 1L; // Placeholder - should get from session
        
        List<OrderResponse> orders = orderService.getCustomerOrders(customerId, null);
        model.addAttribute("orders", orders);
        return "orders";
    }
    
    @GetMapping("/{orderId}")
    public String orderDetails(@PathVariable Long orderId, Model model) {
        // TODO: Get current customer from session for authorization
        OrderResponse order = orderService.getOrderById(orderId, null);
        model.addAttribute("order", order);
        return "order-details";
    }
}