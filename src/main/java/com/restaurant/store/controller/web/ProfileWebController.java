package com.restaurant.store.controller.web;

import com.restaurant.store.dto.response.OrderResponse;
import com.restaurant.store.entity.Customer;
import com.restaurant.store.exception.UnauthorizedException;
import com.restaurant.store.security.AuthHelper;
import com.restaurant.store.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller for customer profile pages.
 */
@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileWebController {

    private final AuthHelper authHelper;
    private final OrderService orderService;

    /**
     * Display customer profile page with order statistics.
     */
    @GetMapping
    public String profile(Model model, RedirectAttributes redirectAttributes) {
        try {
            Customer customer = authHelper.user();
            List<OrderResponse> customerOrders = orderService.getCustomerOrders(customer.getId(), null);

            int totalOrders = customerOrders.size();
            BigDecimal totalSpent = customerOrders.stream()
                    .map(OrderResponse::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            model.addAttribute("customer", customer);
            model.addAttribute("totalOrders", totalOrders);
            model.addAttribute("totalSpent", totalSpent);

            return "profile";
        } catch (UnauthorizedException ex) {
            log.debug("Unauthorized profile access: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Please login to view your profile");
            return "redirect:/login";
        }
    }
}
