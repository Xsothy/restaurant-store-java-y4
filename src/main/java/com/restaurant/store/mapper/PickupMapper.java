package com.restaurant.store.mapper;

import com.restaurant.store.dto.response.PickupResponse;
import com.restaurant.store.entity.Pickup;
import org.springframework.stereotype.Component;

@Component
public class PickupMapper {

    public PickupResponse toResponse(Pickup pickup) {
        if (pickup == null) {
            return null;
        }

        return PickupResponse.builder()
                .id(pickup.getId())
                .orderId(pickup.getOrder() != null ? pickup.getOrder().getId() : null)
                .pickupCode(pickup.getPickupCode())
                .status(pickup.getStatus())
                .readyAt(pickup.getReadyAt())
                .windowStart(pickup.getWindowStart())
                .windowEnd(pickup.getWindowEnd())
                .pickedUpAt(pickup.getPickedUpAt())
                .instructions(pickup.getInstructions())
                .contactName(pickup.getContactName())
                .contactPhone(pickup.getContactPhone())
                .createdAt(pickup.getCreatedAt())
                .updatedAt(pickup.getUpdatedAt())
                .build();
    }
}
