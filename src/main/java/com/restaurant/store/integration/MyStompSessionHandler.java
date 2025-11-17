package com.restaurant.store.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.store.controller.api.DeliveryStatusWebSocketController;
import com.restaurant.store.controller.api.OrderStatusWebSocketController;
import com.restaurant.store.dto.admin.websocket.WebSocketMessageDTO;
import com.restaurant.store.dto.response.OrderStatusMessage;
import com.restaurant.store.entity.Order;
import com.restaurant.store.entity.OrderStatus;
import com.restaurant.store.repository.OrderRepository;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@ConditionalOnProperty(name = "admin.api.websocket.bridge.enabled", havingValue = "true")
public class MyStompSessionHandler extends StompSessionHandlerAdapter {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };

    private final OrderRepository orderRepository;
    private final OrderStatusWebSocketController orderStatusWebSocketController;
    private final DeliveryStatusWebSocketController deliveryStatusWebSocketController;
    private final ObjectMapper objectMapper;
    private final WebSocketStompClient stompClient;
    private final String websocketUrl;
    private final String subscriptionTopic;
    private final String deliverySubscriptionTopic;
    private final StompFrameHandler adminFrameHandler = new AdminEventFrameHandler();
    private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setName("admin-ws-reconnect");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicInteger reconnectAttempts = new AtomicInteger();

    private volatile StompSession session;

    public MyStompSessionHandler(OrderRepository orderRepository,
                                 OrderStatusWebSocketController orderStatusWebSocketController,
                                 DeliveryStatusWebSocketController deliveryStatusWebSocketController,
                                 ObjectMapper objectMapper,
                                 WebSocketStompClient stompClient,
                                 String websocketUrl,
                                 String subscriptionTopic,
                                 String deliverySubscriptionTopic) {
        this.orderRepository = orderRepository;
        this.orderStatusWebSocketController = orderStatusWebSocketController;
        this.deliveryStatusWebSocketController = deliveryStatusWebSocketController;
        this.objectMapper = objectMapper;
        this.stompClient = stompClient;
        this.websocketUrl = websocketUrl;
        this.subscriptionTopic = subscriptionTopic;
        this.deliverySubscriptionTopic = deliverySubscriptionTopic;
    }

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        log.info("Admin WebSocket connected: {}", connectedHeaders);
        reconnectAttempts.set(0);
        this.session = session;
        session.subscribe(subscriptionTopic, adminFrameHandler);
        session.subscribe(deliverySubscriptionTopic, adminFrameHandler);
    }

    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
        log.error("Transport error on Admin WebSocket", exception);
        scheduleReconnect();
    }

    @Override
    public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
        log.error("Admin WebSocket command {} failed", command, exception);
    }

    @Override
    public Type getPayloadType(StompHeaders headers) {
        return WebSocketMessageDTO.class;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        log.debug("Received Admin WebSocket frame for command {}", headers.getCommand());
    }

    @PreDestroy
    public void shutdown() {
        reconnectExecutor.shutdownNow();
        if (session != null && session.isConnected()) {
            try {
                session.disconnect();
            } catch (Exception ex) {
                log.debug("Error disconnecting Admin WebSocket session", ex);
            }
        }
    }

    private void scheduleReconnect() {
        long delaySeconds = Math.min(60, (long) Math.pow(2, reconnectAttempts.incrementAndGet()));
        reconnectExecutor.schedule(() -> {
            log.info("Attempting Admin WebSocket reconnect to {}", websocketUrl);
            try {
                ListenableFuture<StompSession> future = stompClient.connectAsync(websocketUrl, this);
                future.addCallback(
                        stompSession -> {
                            log.info("Admin WebSocket reconnected");
                            this.session = stompSession;
                        },
                        ex -> {
                            log.error("Failed to reconnect Admin WebSocket", ex);
                            scheduleReconnect();
                        }
                );
            } catch (Exception ex) {
                log.error("Unexpected error while reconnecting Admin WebSocket", ex);
                scheduleReconnect();
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    private void handlePayload(WebSocketMessageDTO message) {
        if (message == null) {
            return;
        }

        try {
            forwardAdminMessage(message);
            log.info("Received Admin WebSocket message: {}", message);
        } catch (Exception e) {
            log.error("Failed to process Admin WebSocket payload", e);
        }
    }

    private void forwardAdminMessage(WebSocketMessageDTO message) {
        String type = Optional.ofNullable(message.getType()).orElse("");
        if (type.contains("DELIVERY")) {
            forwardAdminDeliveryMessage(message);
        } else {
            forwardAdminOrderMessage(message);
        }
    }

    private void forwardAdminOrderMessage(WebSocketMessageDTO message) {
        Map<String, Object> data = extractData(message.getData());
        Long adminOrderId = extractOrderId(data);
        if (adminOrderId == null) {
            log.debug("Ignoring Admin WebSocket message without order id: {}", message);
            return;
        }

        Optional<Order> optionalOrder = orderRepository.findByExternalId(adminOrderId);
        if (optionalOrder.isEmpty()) {
            optionalOrder = orderRepository.findById(adminOrderId);
        }

        if (optionalOrder.isEmpty()) {
            log.warn("Received Admin WebSocket event for unknown order id {}", adminOrderId);
            return;
        }

        Order order = optionalOrder.get();
        boolean updated = applyAdminOrderData(order, data);
        if (updated) {
            orderRepository.save(order);
        }

        OrderStatusMessage statusMessage = OrderStatusMessage.builder()
                .orderId(order.getId())
                .status(order.getStatus().name())
                .eventType(resolveEventType(message))
                .title(resolveTitle(message))
                .message(Optional.ofNullable(message.getMessage()).orElse("Order update received"))
                .estimatedDeliveryTime(order.getEstimatedDeliveryTime())
                .timestamp(Optional.ofNullable(message.getTimestamp()).orElse(LocalDateTime.now()))
                .metadata(data.isEmpty() ? Collections.emptyMap() : data)
                .build();

        orderStatusWebSocketController.sendOrderStatusUpdate(order.getId(), statusMessage);
        orderStatusWebSocketController.sendOrderNotification(order.getId(), statusMessage);
    }

    private void forwardAdminDeliveryMessage(WebSocketMessageDTO message) {
        Map<String, Object> data = extractData(message.getData());
        Long adminOrderId = extractOrderId(data);
        if (adminOrderId == null) {
            log.debug("Ignoring Admin WebSocket delivery message without order id: {}", message);
            return;
        }

        Optional<Order> optionalOrder = orderRepository.findByExternalId(adminOrderId);
        if (optionalOrder.isEmpty()) {
            optionalOrder = orderRepository.findById(adminOrderId);
        }

        if (optionalOrder.isEmpty()) {
            log.warn("Received Admin WebSocket delivery event for unknown order id {}", adminOrderId);
            return;
        }

        Order order = optionalOrder.get();
        boolean updated = applyAdminOrderData(order, data);
        if (updated) {
            orderRepository.save(order);
        }

        orderStatusWebSocketController.sendOrderNotification(order.getId(), OrderStatusMessage.builder()
                .orderId(order.getId())
                .status(order.getStatus().name())
                .eventType(resolveEventType(message))
                .title(resolveTitle(message))
                .message(Optional.ofNullable(message.getMessage()).orElse("Delivery update received"))
                .estimatedDeliveryTime(order.getEstimatedDeliveryTime())
                .timestamp(Optional.ofNullable(message.getTimestamp()).orElse(LocalDateTime.now()))
                .metadata(data.isEmpty() ? Collections.emptyMap() : data)
                .build());

        deliveryStatusWebSocketController.sendDeliveryStatusUpdate(order.getId(), message);
    }

    private Map<String, Object> extractData(Object data) {
        if (data == null) {
            return Collections.emptyMap();
        }

        if (data instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .collect(Collectors.toMap(entry -> String.valueOf(entry.getKey()), Map.Entry::getValue));
        }

        try {
            return objectMapper.convertValue(data, MAP_TYPE);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to convert Admin WebSocket payload data: {}", data, e);
            return Collections.emptyMap();
        }
    }

    private Long extractOrderId(Map<String, Object> data) {
        Object rawOrderId = data.getOrDefault("orderId", data.get("id"));
        if (rawOrderId instanceof Number number) {
            return number.longValue();
        }

        if (rawOrderId instanceof String string) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException ex) {
                log.debug("Failed to parse order id {}", string, ex);
            }
        }

        return null;
    }

    private boolean applyAdminOrderData(Order order, Map<String, Object> data) {
        boolean updated = false;

        if (data.containsKey("status")) {
            OrderStatus status = parseStatus(String.valueOf(data.get("status")));
            if (status != null && status != order.getStatus()) {
                order.setStatus(status);
                updated = true;
            }
        }

        if (data.containsKey("estimatedDeliveryTime")) {
            LocalDateTime estimatedDelivery = parseDateTime(String.valueOf(data.get("estimatedDeliveryTime")));
            if (estimatedDelivery != null && !estimatedDelivery.equals(order.getEstimatedDeliveryTime())) {
                order.setEstimatedDeliveryTime(estimatedDelivery);
                updated = true;
            }
        }

        return updated;
    }

    private OrderStatus parseStatus(String status) {
        try {
            return OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.debug("Unknown status {}", status, ex);
            return null;
        }
    }

    private LocalDateTime parseDateTime(String value) {
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException ex) {
            log.debug("Failed to parse timestamp {}", value, ex);
            return null;
        }
    }

    private String resolveEventType(WebSocketMessageDTO message) {
        return Optional.ofNullable(message.getType()).orElse("ORDER_UPDATE");
    }

    private String resolveTitle(WebSocketMessageDTO message) {
        return Optional.ofNullable(message.getTitle()).orElse("Order Update");
    }

    private class AdminEventFrameHandler implements StompFrameHandler {

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return WebSocketMessageDTO.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            handlePayload((WebSocketMessageDTO) payload);
        }
    }
}
