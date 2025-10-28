package com.restaurant.store.dto.response;

import com.restaurant.store.entity.DeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryResponse {
    
    private Long id;
    private Long orderId;
    private String driverName;
    private String driverPhone;
    private String vehicleInfo;
    private DeliveryStatus status;
    private LocalDateTime pickupTime;
    private LocalDateTime estimatedArrivalTime;
    private LocalDateTime actualDeliveryTime;
    private String deliveryNotes;
    private String currentLocation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}