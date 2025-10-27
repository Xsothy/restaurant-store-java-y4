package com.restaurant.store.controller;

import com.restaurant.store.dto.request.CreateOrderRequest;
import com.restaurant.store.dto.request.PaymentRequest;
import com.restaurant.store.dto.response.ApiResponse;
import com.restaurant.store.dto.response.OrderResponse;
import com.restaurant.store.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = "*")
public class OrderController {
    
    @Autowired
    private OrderService orderService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader("Authorization") String authToken) {
        
        OrderResponse response = orderService.createOrder(request, authToken);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", response));
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @PathVariable Long orderId,
            @RequestHeader("Authorization") String authToken) {
        
        OrderResponse response = orderService.getOrderById(orderId, authToken);
        return ResponseEntity.ok(ApiResponse.success("Order retrieved successfully", response));
    }
    
    @GetMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<String>> getOrderStatus(
            @PathVariable Long orderId,
            @RequestHeader("Authorization") String authToken) {
        
        String status = orderService.getOrderStatus(orderId, authToken);
        return ResponseEntity.ok(ApiResponse.success("Order status retrieved successfully", status));
    }
    
    @PostMapping("/{orderId}/pay")
    public ResponseEntity<ApiResponse<String>> payOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentRequest request,
            @RequestHeader("Authorization") String authToken) {
        
        String result = orderService.processPayment(orderId, request, authToken);
        return ResponseEntity.ok(ApiResponse.success(result, null));
    }
    
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getCustomerOrders(
            @PathVariable Long customerId,
            @RequestHeader("Authorization") String authToken) {
        
        List<OrderResponse> orders = orderService.getCustomerOrders(customerId, authToken);
        return ResponseEntity.ok(ApiResponse.success("Customer orders retrieved successfully", orders));
    }
    
    @GetMapping("/my-orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(
            @RequestHeader("Authorization") String authToken) {
        
        List<OrderResponse> orders = orderService.getMyOrders(authToken);
        return ResponseEntity.ok(ApiResponse.success("Your orders retrieved successfully", orders));
    }
    
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<String>> cancelOrder(
            @PathVariable Long orderId,
            @RequestHeader("Authorization") String authToken) {
        
        String result = orderService.cancelOrder(orderId, authToken);
        return ResponseEntity.ok(ApiResponse.success(result, null));
    }
}