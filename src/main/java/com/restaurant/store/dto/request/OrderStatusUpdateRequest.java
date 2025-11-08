package com.restaurant.store.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderStatusUpdateRequest {
    private String status;
    private LocalDateTime estimatedDeliveryTime;
}
