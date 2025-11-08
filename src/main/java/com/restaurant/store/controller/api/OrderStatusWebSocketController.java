package com.restaurant.store.controller.api;

import com.restaurant.store.dto.response.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class OrderStatusWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/orders/{orderId}/subscribe")
    @SendTo("/topic/orders/{orderId}")
    public String subscribeToOrder(@DestinationVariable Long orderId) {
        log.info("Client subscribed to order updates: {}", orderId);
        return "Subscribed to order " + orderId;
    }

    public void sendOrderUpdate(Long orderId, OrderResponse orderResponse) {
        log.info("Sending order update for order: {}", orderId);
        messagingTemplate.convertAndSend("/topic/orders/" + orderId, orderResponse);
    }

    public void sendOrderStatusUpdate(Long orderId, String status) {
        log.info("Sending status update for order: {} - Status: {}", orderId, status);
        messagingTemplate.convertAndSend("/topic/orders/" + orderId + "/status", status);
    }
}
