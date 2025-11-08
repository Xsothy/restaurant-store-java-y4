package com.restaurant.store.controller.web;

import com.restaurant.store.dto.response.PaymentResponse;
import com.restaurant.store.entity.Customer;
import com.restaurant.store.entity.Order;
import com.restaurant.store.exception.BadRequestException;
import com.restaurant.store.exception.ResourceNotFoundException;
import com.restaurant.store.repository.CustomerRepository;
import com.restaurant.store.repository.OrderRepository;
import com.restaurant.store.security.JwtUtil;
import com.restaurant.store.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/web/payment")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Web Payment", description = "Web payment endpoints that support both Payment Intent and Payment Session")
@SecurityRequirement(name = "bearerAuth")
public class WebPaymentController {

    private final PaymentService paymentService;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final JwtUtil jwtUtil;

    @Operation(
            summary = "Create payment (Web)",
            description = "Creates a payment for an order. Returns Payment Intent (custom UI) or Payment Session (Stripe hosted) based on configuration."
    )
    @PostMapping("/create/{orderId}")
    public ResponseEntity<Map<String, Object>> createPayment(
            @Parameter(description = "Order ID") @PathVariable Long orderId,
            @RequestHeader("Authorization") String authToken) {
        
        String email = jwtUtil.extractUsername(authToken.substring(7));
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new BadRequestException("Order does not belong to current customer");
        }

        try {
            PaymentResponse response = paymentService.createPayment(order);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);
            
            log.info("Payment created for order {} using service type: {}", orderId, paymentService.getServiceType());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error creating payment for order {}", orderId, e);
            throw new BadRequestException("Failed to create payment: " + e.getMessage());
        }
    }
}
