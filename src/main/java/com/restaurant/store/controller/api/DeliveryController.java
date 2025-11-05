package com.restaurant.store.controller.api;

import com.restaurant.store.dto.response.DeliveryResponse;
import com.restaurant.store.service.DeliveryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/deliveries")
@CrossOrigin(origins = "*")
public class DeliveryController {
    
    @Autowired
    private DeliveryService deliveryService;
    
    @GetMapping("/{orderId}")
    public DeliveryResponse getDeliveryByOrderId(
            @PathVariable Long orderId,
            @RequestHeader("Authorization") String authToken) {
        
        return deliveryService.getDeliveryByOrderId(orderId, authToken);
    }
    
    @GetMapping("/track/{orderId}")
    public DeliveryResponse trackDelivery(
            @PathVariable Long orderId,
            @RequestHeader("Authorization") String authToken) {
        
        return deliveryService.trackDelivery(orderId, authToken);
    }
    
    @PutMapping("/{deliveryId}/update-location")
    public String updateDeliveryLocation(
            @PathVariable Long deliveryId,
            @RequestParam String location,
            @RequestHeader("Authorization") String authToken) {
        
        // TODO: Implement delivery location update logic (for driver/admin use)
        return "Delivery location updated successfully";
    }
    
    @PutMapping("/{deliveryId}/update-status")
    public String updateDeliveryStatus(
            @PathVariable Long deliveryId,
            @RequestParam String status,
            @RequestHeader("Authorization") String authToken) {
        
        // TODO: Implement delivery status update logic (for driver/admin use)
        return "Delivery status updated successfully";
    }
}