package com.restaurant.store.controller.web;

import com.restaurant.store.dto.response.CustomerResponse;
import com.restaurant.store.dto.response.OrderResponse;
import com.restaurant.store.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/orders")
public class OrderWebController {
    
    @Autowired
    private OrderService orderService;
    
    @GetMapping
    public String orders(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("customer") == null) {
            return "redirect:/auth/login";
        }
        
        CustomerResponse customer = (CustomerResponse) session.getAttribute("customer");
        List<OrderResponse> orders = orderService.getCustomerOrders(customer.getId(), null);
        model.addAttribute("orders", orders);
        return "orders";
    }
    
    @GetMapping("/{orderId}")
    public String orderDetails(@PathVariable Long orderId, HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("customer") == null) {
            return "redirect:/auth/login";
        }
        
        OrderResponse order = orderService.getOrderById(orderId, null);
        model.addAttribute("order", order);
        return "order-details";
    }
}