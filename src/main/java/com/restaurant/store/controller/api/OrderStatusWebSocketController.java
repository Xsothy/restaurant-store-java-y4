package com.restaurant.store.controller.api;

import com.restaurant.store.dto.response.OrderResponse;
import com.restaurant.store.dto.response.OrderStatusMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@Slf4j
public class OrderStatusWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/orders/{orderId}/subscribe")
    @SendTo("/topic/orders/{orderId}")
    public OrderStatusMessage subscribeToOrder(@DestinationVariable Long orderId) {
        log.info("Client subscribed to order updates: {}", orderId);
        return OrderStatusMessage.builder()
                .orderId(orderId)
                .eventType("SUBSCRIPTION_CONFIRMED")
                .status("SUBSCRIBED")
                .title("Subscription confirmed")
                .message("Subscribed to order " + orderId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public void sendOrderUpdate(Long orderId, OrderResponse orderResponse) {
        log.info("Sending order update for order: {}", orderId);
        messagingTemplate.convertAndSend("/topic/orders/" + orderId, orderResponse);
    }

    public void sendOrderStatusUpdate(Long orderId, OrderStatusMessage statusMessage) {
        OrderStatusMessage payload = statusMessage != null
                ? statusMessage
                : OrderStatusMessage.builder().build();

        payload.setOrderId(orderId);
        if (payload.getTimestamp() == null) {
            payload.setTimestamp(LocalDateTime.now());
        }

        log.info("Sending status update for order: {} - Event: {} - Status: {}", orderId,
                payload.getEventType(), payload.getStatus());
        messagingTemplate.convertAndSend("/topic/orders/" + orderId + "/status", payload);
    }

    public void sendOrderNotification(Long orderId, OrderStatusMessage notification) {
        OrderStatusMessage payload = notification != null
                ? notification
                : OrderStatusMessage.builder().build();

        payload.setOrderId(orderId);
        if (payload.getTimestamp() == null) {
            payload.setTimestamp(LocalDateTime.now());
        }

        log.info("Sending notification for order: {} - {}", orderId, payload.getEventType());
        messagingTemplate.convertAndSend("/topic/orders/" + orderId + "/notifications", payload);
    }
}
