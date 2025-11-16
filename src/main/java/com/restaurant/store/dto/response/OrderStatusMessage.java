package com.restaurant.store.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents a WebSocket payload for order tracking updates consumed by the mobile app.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusMessage {
    private Long orderId;
    private String status;
    private String eventType;
    private String title;
    private String message;
    private LocalDateTime estimatedDeliveryTime;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
}
