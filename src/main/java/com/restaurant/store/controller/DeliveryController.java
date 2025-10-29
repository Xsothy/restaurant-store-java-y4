package com.restaurant.store.controller;

import com.restaurant.store.dto.response.ApiResponse;
import com.restaurant.store.dto.response.DeliveryResponse;
import com.restaurant.store.service.DeliveryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/deliveries")
@CrossOrigin(origins = "*")
public class DeliveryController {
    
    @Autowired
    private DeliveryService deliveryService;
    
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<DeliveryResponse>> getDeliveryByOrderId(
            @PathVariable Long orderId,
            @RequestHeader("Authorization") String authToken) {
        
        DeliveryResponse response = deliveryService.getDeliveryByOrderId(orderId, authToken);
        return ResponseEntity.ok(ApiResponse.success("Delivery information retrieved successfully", response));
    }
    
    @GetMapping("/track/{orderId}")
    public ResponseEntity<ApiResponse<DeliveryResponse>> trackDelivery(
            @PathVariable Long orderId,
            @RequestHeader("Authorization") String authToken) {
        
        DeliveryResponse response = deliveryService.trackDelivery(orderId, authToken);
        return ResponseEntity.ok(ApiResponse.success("Delivery tracking information retrieved successfully", response));
    }
    
    @PutMapping("/{deliveryId}/update-location")
    public ResponseEntity<ApiResponse<String>> updateDeliveryLocation(
            @PathVariable Long deliveryId,
            @RequestParam String location,
            @RequestHeader("Authorization") String authToken) {
        
        // TODO: Implement delivery location update logic (for driver/admin use)
        return ResponseEntity.ok(ApiResponse.success("Delivery location updated successfully", null));
    }
    
    @PutMapping("/{deliveryId}/update-status")
    public ResponseEntity<ApiResponse<String>> updateDeliveryStatus(
            @PathVariable Long deliveryId,
            @RequestParam String status,
            @RequestHeader("Authorization") String authToken) {
        
        // TODO: Implement delivery status update logic (for driver/admin use)
        return ResponseEntity.ok(ApiResponse.success("Delivery status updated successfully", null));
    }
}