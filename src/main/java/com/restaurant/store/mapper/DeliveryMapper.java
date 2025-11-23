package com.restaurant.store.mapper;

import com.restaurant.store.dto.response.DeliveryResponse;
import com.restaurant.store.entity.Delivery;
import org.springframework.stereotype.Component;

@Component
public class DeliveryMapper {
    
    public DeliveryResponse toResponse(Delivery delivery) {
        if (delivery == null) {
            return null;
        }
        
        return DeliveryResponse.builder()
                .id(delivery.getId())
                .orderId(delivery.getOrder() != null ? delivery.getOrder().getId() : null)
                .driverName(delivery.getDriverName())
                .driverPhone(delivery.getDriverPhone())
                .vehicleInfo(delivery.getVehicleInfo())
                .status(delivery.getStatus())
                .pickupTime(delivery.getPickupTime())
                .estimatedArrivalTime(delivery.getEstimatedArrivalTime())
                .actualDeliveryTime(delivery.getActualDeliveryTime())
                .deliveryNotes(delivery.getDeliveryNotes())
                .currentLocation(delivery.getCurrentLocation())
                .latitude(delivery.getLatitude())
                .longitude(delivery.getLongitude())
                .createdAt(delivery.getCreatedAt())
                .updatedAt(delivery.getUpdatedAt())
                .build();
    }
}
