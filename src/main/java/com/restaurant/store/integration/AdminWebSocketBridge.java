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
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "admin.api.websocket.bridge.enabled", havingValue = "true")
@Slf4j
public class AdminWebSocketBridge {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };

    private final OrderRepository orderRepository;
    private final OrderStatusWebSocketController orderStatusWebSocketController;
    private final DeliveryStatusWebSocketController deliveryStatusWebSocketController;
    private final ObjectMapper objectMapper;
    private final WebSocketStompClient stompClient;
    private final ThreadPoolTaskScheduler taskScheduler;
    private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setName("admin-ws-reconnect");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicInteger reconnectAttempts = new AtomicInteger();
    private final String websocketUrl;
    private final String subscriptionTopic;
    private final String deliverySubscriptionTopic;

    private volatile StompSession session;

    public AdminWebSocketBridge(OrderRepository orderRepository,
                                OrderStatusWebSocketController orderStatusWebSocketController,
                                DeliveryStatusWebSocketController deliveryStatusWebSocketController,
                                ObjectMapper objectMapper,
                                @Value("${admin.api.websocket.url}") String websocketUrl,
                                @Value("${admin.api.websocket.topic:/topic/admin/orders}") String subscriptionTopic,
                                @Value("${admin.api.websocket.delivery-topic:/topic/deliveries}") String deliverySubscriptionTopic) {
        this.orderRepository = orderRepository;
        this.orderStatusWebSocketController = orderStatusWebSocketController;
        this.deliveryStatusWebSocketController = deliveryStatusWebSocketController;
        this.objectMapper = objectMapper;
        this.websocketUrl = websocketUrl;
        this.subscriptionTopic = subscriptionTopic;
        this.deliverySubscriptionTopic = deliverySubscriptionTopic;
        this.taskScheduler = new ThreadPoolTaskScheduler();
        this.taskScheduler.setThreadNamePrefix("admin-ws-heartbeat-");
        this.taskScheduler.initialize();
        this.stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        this.stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        this.stompClient.setTaskScheduler(taskScheduler);
        this.stompClient.setDefaultHeartbeat(new long[]{10000, 10000});
    }

    @PostConstruct
    public void startBridge() {
        log.info("Admin WebSocket bridge enabled. Connecting to {}", websocketUrl);
        scheduleConnect(Duration.ZERO);
    }

    @PreDestroy
    public void shutdown() {
        log.info("Stopping Admin WebSocket bridge");
        if (session != null && session.isConnected()) {
            try {
                session.disconnect();
            } catch (Exception e) {
                log.debug("Error disconnecting Admin WebSocket session", e);
            }
        }
        stompClient.stop();
        taskScheduler.shutdown();
        reconnectExecutor.shutdownNow();
    }

    private void scheduleConnect(Duration delay) {
        reconnectExecutor.schedule(() -> {
            try {
                stompClient.connectAsync(websocketUrl, new AdminSessionHandler());
            } catch (Exception e) {
                log.error("Failed to initiate Admin WebSocket connection", e);
                scheduleReconnect();
            }
        }, delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void scheduleReconnect() {
        long delaySeconds = Math.min(60, (long) Math.pow(2, reconnectAttempts.incrementAndGet()));
        log.warn("Scheduling Admin WebSocket reconnect in {} seconds", delaySeconds);
        scheduleConnect(Duration.ofSeconds(delaySeconds));
    }

    private void handlePayload(byte[] payload) {
        if (payload == null || payload.length == 0) {
            return;
        }

        try {
            WebSocketMessageDTO dto = objectMapper.readValue(payload, WebSocketMessageDTO.class);
            forwardAdminMessage(dto);
            log.info("Websocket message", dto);
        } catch (Exception e) {
            log.error("Failed to parse Admin WebSocket payload", e);
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
        Long orderId = extractOrderId(data);
        if (orderId == null) {
            log.debug("Ignoring Admin delivery WebSocket message without order id: {}", message);
            return;
        }

        OrderStatusMessage statusMessage = OrderStatusMessage.builder()
                .orderId(orderId)
                .status(Optional.ofNullable(extractString(data, List.of("status", "deliveryStatus")))
                        .orElse("UPDATED"))
                .eventType(Optional.ofNullable(message.getType()).orElse("DELIVERY_STATUS_UPDATED"))
                .title("Delivery status updated")
                .message(Optional.ofNullable(message.getMessage()).orElse("Delivery update received"))
                .timestamp(Optional.ofNullable(message.getTimestamp()).orElse(LocalDateTime.now()))
                .metadata(data.isEmpty() ? Collections.emptyMap() : data)
                .build();

        deliveryStatusWebSocketController.sendDeliveryStatusUpdate(orderId, statusMessage);
        deliveryStatusWebSocketController.sendDeliveryNotification(orderId, statusMessage);
    }

    private boolean applyAdminOrderData(Order order, Map<String, Object> data) {
        boolean updated = false;
        String statusValue = extractString(data, List.of("status", "orderStatus"));
        if (statusValue != null) {
            try {
                OrderStatus status = OrderStatus.valueOf(statusValue.toUpperCase());
                if (status != order.getStatus()) {
                    order.setStatus(status);
                    updated = true;
                }
            } catch (IllegalArgumentException e) {
                log.warn("Unknown order status '{}' from Admin WebSocket", statusValue);
            }
        }

        String etaValue = extractString(data, List.of("estimatedDeliveryTime", "eta"));
        if (etaValue != null) {
            try {
                order.setEstimatedDeliveryTime(LocalDateTime.parse(etaValue));
                updated = true;
            } catch (DateTimeParseException e) {
                log.debug("Unable to parse ETA '{}' from Admin WebSocket", etaValue);
            }
        }

        return updated;
    }

    private String resolveEventType(WebSocketMessageDTO message) {
        return Optional.ofNullable(message.getType()).orElse("ADMIN_EVENT");
    }

    private String resolveTitle(WebSocketMessageDTO message) {
        return switch (Optional.ofNullable(message.getType()).orElse("")) {
            case "ORDER_STATUS_CHANGED" -> "Order status updated";
            case "ORDER_UPDATED" -> "Order updated";
            case "ORDER_CREATED" -> "Order acknowledged";
            default -> "Admin update";
        };
    }

    private Map<String, Object> extractData(Object payload) {
        if (payload == null) {
            return Collections.emptyMap();
        }

        if (payload instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .collect(Collectors.toMap(entry -> String.valueOf(entry.getKey()), Map.Entry::getValue));
        }

        try {
            return objectMapper.convertValue(payload, MAP_TYPE);
        } catch (IllegalArgumentException e) {
            log.debug("Unable to convert Admin WebSocket payload to map", e);
            return Collections.emptyMap();
        }
    }

    private Long extractOrderId(Map<String, Object> data) {
        for (String key : List.of("orderId", "id", "externalId", "order_id", "orderID")) {
            Object value = data.get(key);
            Long parsed = convertToLong(value);
            if (parsed != null) {
                return parsed;
            }
        }

        Object nestedOrder = data.get("order");
        if (nestedOrder != null) {
            return extractOrderId(extractData(nestedOrder));
        }

        return null;
    }

    private String extractString(Map<String, Object> data, List<String> keys) {
        for (String key : keys) {
            Object value = data.get(key);
            if (value instanceof String str && !str.isBlank()) {
                return str;
            }
        }
        return null;
    }

    private Long convertToLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private class AdminSessionHandler extends StompSessionHandlerAdapter {
        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            log.info("Connected to Admin WebSocket endpoint");
            reconnectAttempts.set(0);
            AdminWebSocketBridge.this.session = session;
            session.subscribe(subscriptionTopic, this);
            log.info("Subscribed to Admin WebSocket topic {}", subscriptionTopic);
            if (!deliverySubscriptionTopic.equals(subscriptionTopic)) {
                session.subscribe(deliverySubscriptionTopic, this);
                log.info("Subscribed to Admin WebSocket delivery topic {}", deliverySubscriptionTopic);
            }
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return byte[].class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            handlePayload((byte[]) payload);
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            log.error("Admin WebSocket transport error", exception);
            scheduleReconnect();
        }

        @Override
        public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload,
                                    Throwable exception) {
            log.error("Admin WebSocket exception", exception);
        }
    }
}
