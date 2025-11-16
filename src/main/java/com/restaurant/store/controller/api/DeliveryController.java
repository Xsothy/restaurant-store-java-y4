package com.restaurant.store.controller.api;

import com.restaurant.store.dto.response.ApiResponse;
import com.restaurant.store.dto.response.DeliveryResponse;
import com.restaurant.store.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API controller for delivery management and tracking.
 * 
 * Note: This controller provides REST endpoints for polling delivery status.
 * For real-time delivery tracking, use WebSocket endpoints instead.
 * See /api/websocket/info for WebSocket connection details.
 */
@RestController
@RequestMapping("/api/deliveries")
@CrossOrigin(origins = "*")
@Tag(name = "Deliveries", description = "Delivery tracking and management (REST). For real-time updates, use WebSocket at /ws")
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
    @Operation(
        summary = "Track Delivery (REST/Polling)",
        description = """
            Get current delivery status for an order via REST API.
            This endpoint is for polling-based tracking.
            
            **For Real-Time Tracking:** Use WebSocket instead!
            - Connect to: ws://localhost:8080/ws
            - Subscribe to: /topic/deliveries/{orderId}
            - See /api/websocket/info for full documentation
            
            This REST endpoint should only be used if WebSocket is not available.
            """
    )
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
