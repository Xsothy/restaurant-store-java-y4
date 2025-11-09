package com.restaurant.store.controller.web;

import com.restaurant.store.dto.response.CustomerResponse;
import com.restaurant.store.dto.response.OrderResponse;
import com.restaurant.store.service.OrderService;
import com.restaurant.store.repository.CustomerRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.restaurant.store.exception.UnauthorizedException;
import com.restaurant.store.entity.Customer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/orders")
public class OrderWebController {
    
    private final OrderService orderService;
    private final CustomerRepository customerRepository;

    @GetMapping
    public String orders(HttpServletRequest request, Model model, @CookieValue(value = "jwt", required = false) String jwtCookie) {
        try {
            String token = getAuthToken(jwtCookie);
            Customer customer = getCustomerFromAuth();
            List<OrderResponse> orders = orderService.getCustomerOrders(customer.getId(), token);
            model.addAttribute("orders", orders);
            return "orders";
        } catch (UnauthorizedException e) {
            return "redirect:/auth/login";
        }
    }
    
    @GetMapping("/{orderId}")
    public String orderDetails(@PathVariable Long orderId, HttpServletRequest request, Model model, @CookieValue(value = "jwt", required = false) String jwtCookie) {
        try {
            String token = getAuthToken(jwtCookie);
            Customer customer = getCustomerFromAuth();

            OrderResponse order = orderService.getOrderById(orderId, token);
            model.addAttribute("order", order);
            return "order-details";
        } catch (UnauthorizedException e) {
            return "redirect:/auth/login";
        }

    }

    private String getAuthToken(String jwtCookie) {
        if (jwtCookie != null && !jwtCookie.isEmpty()) {
            return "Bearer " + jwtCookie;
        }
        throw new UnauthorizedException("Authentication required");
    }

    private Customer getCustomerFromAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            return customerRepository.findByEmail(email)
                    .orElseThrow(() -> new UnauthorizedException("Customer not found"));
        }
        throw new UnauthorizedException("Authentication required");
    }
}