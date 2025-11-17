package com.restaurant.store.dto.admin.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessageDTO<T> {
    private String type;
    private String title;
    private String message;
    private T data;
    private String userId;
    private LocalDateTime timestamp;
    
    public enum MessageType {
        ORDER_CREATED,
        ORDER_UPDATED,
        ORDER_STATUS_CHANGED,
        DELIVERY_ASSIGNED,
        DELIVERY_STATUS_UPDATED,
        USER_NOTIFICATION,
        SYSTEM_ALERT
    }
}