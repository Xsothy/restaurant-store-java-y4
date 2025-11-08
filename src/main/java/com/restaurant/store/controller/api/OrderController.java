package com.restaurant.store.controller.api;

import com.restaurant.store.dto.request.CreateOrderRequest;
import com.restaurant.store.dto.request.PaymentRequest;
import com.restaurant.store.dto.response.OrderResponse;
import com.restaurant.store.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader("Authorization") String authToken) {
        
        OrderResponse response = orderService.createOrder(request, authToken);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{orderId}")
    public OrderResponse getOrder(
            @PathVariable Long orderId,
            @RequestHeader("Authorization") String authToken) {
        
        return orderService.getOrderById(orderId, authToken);
    }
    
    @GetMapping("/{orderId}/status")
    public String getOrderStatus(
            @PathVariable Long orderId,
            @RequestHeader("Authorization") String authToken) {
        
        return orderService.getOrderStatus(orderId, authToken);
    }
    
    @PostMapping("/{orderId}/pay")
    public String payOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader("Authorization") String authToken) {
        
        return orderService.processPayment(orderId, request, authToken);
    }
    
    @GetMapping("/customer/{customerId}")
    public List<OrderResponse> getCustomerOrders(
            @PathVariable Long customerId,
            @RequestHeader("Authorization") String authToken) {
        
        return orderService.getCustomerOrders(customerId, authToken);
    }
    
    @GetMapping("/my-orders")
    public List<OrderResponse> getMyOrders(@RequestHeader("Authorization") String authToken) {
        return orderService.getMyOrders(authToken);
    }
    
    @PutMapping("/{orderId}/cancel")
    public String cancelOrder(
            @PathVariable Long orderId,
            @RequestHeader("Authorization") String authToken) {
        
        return orderService.cancelOrder(orderId, authToken);
    }
}