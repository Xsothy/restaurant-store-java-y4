package com.restaurant.store.controller.api;

import com.restaurant.store.dto.response.ApiResponse;
import com.restaurant.store.dto.response.DeliveryResponse;
import com.restaurant.store.entity.DeliveryStatus;
import com.restaurant.store.service.DeliveryService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
    
    @PostMapping("/{orderId}/status")
    @Hidden
    @Operation(summary = "Update Delivery Status (Admin/Driver Only)", 
               description = "Updates delivery status and broadcasts via WebSocket. This endpoint is for admin/driver use only.")
    public ResponseEntity<ApiResponse<DeliveryResponse>> updateDeliveryStatus(
            @PathVariable Long orderId,
            @RequestParam String status,
            @RequestParam(required = false) String location) {

        log.info("Received delivery status update for order: {} - New status: {}, Location: {}", 
                orderId, status, location);

        DeliveryStatus deliveryStatus = DeliveryStatus.valueOf(status);
        DeliveryResponse response = deliveryService.updateDeliveryStatus(orderId, deliveryStatus, location);

        return ResponseEntity.ok(ApiResponse.success(
                "Delivery status updated successfully",
                response
        ));
    }

    @PostMapping("/{orderId}/location")
    @Hidden
    @Operation(summary = "Update Delivery Location (Driver Only)", 
               description = "Updates driver location and broadcasts via WebSocket. This endpoint is for driver use only.")
    public ResponseEntity<ApiResponse<String>> updateDeliveryLocation(
            @PathVariable Long orderId,
            @RequestParam String location) {

        log.info("Received delivery location update for order: {} - Location: {}", orderId, location);

        deliveryService.updateDeliveryLocation(orderId, location);

        return ResponseEntity.ok(ApiResponse.success(
                "Delivery location updated successfully",
                "Location updated to: " + location
        ));
    }

    @PostMapping("/{orderId}/driver")
    @Hidden
    @Operation(summary = "Assign Driver to Delivery (Admin Only)", 
               description = "Assigns a driver to a delivery and broadcasts notification via WebSocket. This endpoint is for admin use only.")
    public ResponseEntity<ApiResponse<DeliveryResponse>> assignDriver(
            @PathVariable Long orderId,
            @RequestParam String driverName,
            @RequestParam String driverPhone,
            @RequestParam(required = false) String vehicleInfo) {

        log.info("Assigning driver to delivery for order: {} - Driver: {}", orderId, driverName);

        DeliveryResponse response = deliveryService.assignDriver(orderId, driverName, driverPhone, vehicleInfo);

        return ResponseEntity.ok(ApiResponse.success(
                "Driver assigned successfully",
                response
        ));
    }
}
