package com.restaurant.store.controller.api;

import com.restaurant.store.dto.response.DeliveryResponse;
import com.restaurant.store.dto.response.OrderStatusMessage;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

/**
 * WebSocket controller for real-time delivery tracking.
 * Provides STOMP endpoints for subscribing to delivery status updates.
 * 
 * WebSocket Connection:
 * - Connect to: ws://localhost:8080/ws
 * - Use SockJS: new SockJS('http://localhost:8080/ws')
 * - Use STOMP client over SockJS
 * 
 * Topics:
 * - /topic/deliveries/{orderId} - Delivery status updates for an order
 * - /topic/deliveries/{orderId}/location - Real-time location updates
 * 
 * Example (JavaScript):
 * const socket = new SockJS('http://localhost:8080/ws');
 * const stompClient = Stomp.over(socket);
 * stompClient.connect({}, () => {
 *     stompClient.subscribe('/topic/deliveries/506', (message) => {
 *         const delivery = JSON.parse(message.body);
 *         console.log('Delivery update:', delivery);
 *     });
 *     stompClient.send('/app/deliveries/506/subscribe', {}, {});
 * });
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@Hidden
public class DeliveryStatusWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Subscribe to delivery updates for a specific order.
     * Client sends to: /app/deliveries/{orderId}/subscribe
     * Receives on: /topic/deliveries/{orderId}
     */
    @MessageMapping("/deliveries/{orderId}/subscribe")
    @SendTo("/topic/deliveries/{orderId}")
    public OrderStatusMessage subscribeToDelivery(@DestinationVariable Long orderId) {
        log.info("Client subscribed to delivery tracking for order: {}", orderId);
        return OrderStatusMessage.builder()
                .orderId(orderId)
                .eventType("SUBSCRIPTION_CONFIRMED")
                .status("SUBSCRIBED")
                .title("Tracking Active")
                .message("Now tracking delivery for order " + orderId)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Send delivery status update to all subscribers of an order.
     * Used internally by services to broadcast updates.
     */
    public void sendDeliveryUpdate(Long orderId, DeliveryResponse deliveryResponse) {
        log.info("Broadcasting delivery update for order: {} - Status: {}", 
                orderId, deliveryResponse.getStatus());
        messagingTemplate.convertAndSend("/topic/deliveries/" + orderId, deliveryResponse);
    }

    /**
     * Send delivery status message to all subscribers.
     * Used for status changes like "Out for delivery", "Delivered", etc.
     */
    public void sendDeliveryStatusUpdate(Long orderId, OrderStatusMessage statusMessage) {
        OrderStatusMessage payload = statusMessage != null
                ? statusMessage
                : OrderStatusMessage.builder().build();

        payload.setOrderId(orderId);
        if (payload.getTimestamp() == null) {
            payload.setTimestamp(LocalDateTime.now());
        }

        log.info("Broadcasting delivery status for order: {} - Event: {} - Status: {}", 
                orderId, payload.getEventType(), payload.getStatus());
        messagingTemplate.convertAndSend("/topic/deliveries/" + orderId + "/status", payload);
    }

    /**
     * Send real-time location update for delivery.
     * Used by delivery driver app to update location.
     */
    public void sendLocationUpdate(Long orderId, String location) {
        OrderStatusMessage locationUpdate = OrderStatusMessage.builder()
                .orderId(orderId)
                .eventType("LOCATION_UPDATE")
                .status(location)
                .title("Location Updated")
                .message("Delivery location: " + location)
                .timestamp(LocalDateTime.now())
                .build();

        log.info("Broadcasting location update for order: {} - Location: {}", orderId, location);
        messagingTemplate.convertAndSend("/topic/deliveries/" + orderId + "/location", locationUpdate);
    }

    /**
     * Send delivery notification to subscribers.
     * Used for important events like "Driver assigned", "Arriving soon", etc.
     */
    public void sendDeliveryNotification(Long orderId, OrderStatusMessage notification) {
        OrderStatusMessage payload = notification != null
                ? notification
                : OrderStatusMessage.builder().build();

        payload.setOrderId(orderId);
        if (payload.getTimestamp() == null) {
            payload.setTimestamp(LocalDateTime.now());
        }

        log.info("Broadcasting delivery notification for order: {} - {}", orderId, payload.getEventType());
        messagingTemplate.convertAndSend("/topic/deliveries/" + orderId + "/notifications", payload);
    }
}
