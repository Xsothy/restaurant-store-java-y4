package com.restaurant.store.dto.response;

import com.restaurant.store.entity.PickupStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PickupResponse {
    private Long id;
    private Long orderId;
    private String pickupCode;
    private PickupStatus status;
    private LocalDateTime readyAt;
    private LocalDateTime windowStart;
    private LocalDateTime windowEnd;
    private LocalDateTime pickedUpAt;
    private String instructions;
    private String contactName;
    private String contactPhone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
