package com.restaurant.store.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.store.controller.api.DeliveryStatusWebSocketController;
import com.restaurant.store.controller.api.OrderStatusWebSocketController;
import com.restaurant.store.dto.admin.DeliveryDTO;
import com.restaurant.store.dto.admin.OrderDTO;
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
import org.springframework.util.StringUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@ConditionalOnProperty(name = "admin.api.websocket.bridge.enabled", havingValue = "true")
public class MyStompSessionHandler extends StompSessionHandlerAdapter {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };
    private static final EnumSet<WebSocketMessageDTO.MessageType> ORDER_MESSAGE_TYPES = EnumSet.of(
            WebSocketMessageDTO.MessageType.ORDER_CREATED,
            WebSocketMessageDTO.MessageType.ORDER_UPDATED,
            WebSocketMessageDTO.MessageType.ORDER_STATUS_CHANGED
    );
    private static final EnumSet<WebSocketMessageDTO.MessageType> DELIVERY_MESSAGE_TYPES = EnumSet.of(
            WebSocketMessageDTO.MessageType.DELIVERY_ASSIGNED,
            WebSocketMessageDTO.MessageType.DELIVERY_STATUS_UPDATED
    );

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
                CompletableFuture<StompSession> future = stompClient.connectAsync(websocketUrl, this);
                future.whenComplete((stompSession, throwable) -> {
                    if (throwable != null) {
                        log.error("Failed to reconnect Admin WebSocket", throwable);
                        scheduleReconnect();
                        return;
                    }

                    log.info("Admin WebSocket reconnected");
                    this.session = stompSession;
                });
            } catch (Exception ex) {
                log.error("Unexpected error while reconnecting Admin WebSocket", ex);
                scheduleReconnect();
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    private void handlePayload(WebSocketMessageDTO<?> message) {
        if (message == null) {
            return;
        }

        try {
            forwardAdminMessage(message);
            log.info("Received Admin WebSocket message: {}", message.getType());
        } catch (Exception e) {
            log.error("Failed to process Admin WebSocket payload", e);
        }
    }

    private void forwardAdminMessage(WebSocketMessageDTO<?> message) {
        WebSocketMessageDTO.MessageType messageType = resolveMessageType(message.getType());
        if (messageType == null) {
            log.warn("Ignoring Admin WebSocket payload with unknown type: {}", message.getType());
            return;
        }

        if (ORDER_MESSAGE_TYPES.contains(messageType)) {
            forwardAdminOrderMessage(message, messageType);
            return;
        }

        if (DELIVERY_MESSAGE_TYPES.contains(messageType)) {
            forwardAdminDeliveryMessage(message, messageType);
            return;
        }

        log.debug("Admin WebSocket message type {} does not require forwarding", messageType);
    }

    private void forwardAdminOrderMessage(WebSocketMessageDTO<?> message, WebSocketMessageDTO.MessageType messageType) {
        OrderDTO orderData = convertPayload(message.getData(), OrderDTO.class);
        if (orderData == null) {
            log.debug("Ignoring Admin order message without convertible payload: {}", message.getData());
            return;
        }

        Map<String, Object> metadata = convertDataToMap(orderData);
        Long adminOrderId = Optional.ofNullable(orderData.getId())
                .orElseGet(() -> extractOrderId(metadata));
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
        boolean updated = applyAdminOrderData(order, metadata);
        if (updated) {
            orderRepository.save(order);
        }

        OrderStatusMessage statusMessage = buildStatusMessage(order, message, messageType, metadata, "Order update received");
        orderStatusWebSocketController.sendOrderStatusUpdate(order.getId(), statusMessage);
        orderStatusWebSocketController.sendOrderNotification(order.getId(), statusMessage);
    }

    private void forwardAdminDeliveryMessage(WebSocketMessageDTO<?> message, WebSocketMessageDTO.MessageType messageType) {
        DeliveryDTO deliveryData = convertPayload(message.getData(), DeliveryDTO.class);
        if (deliveryData == null) {
            log.debug("Ignoring Admin delivery message without convertible payload: {}", message.getData());
            return;
        }

        Map<String, Object> metadata = convertDataToMap(deliveryData);
        Long adminOrderId = Optional.ofNullable(deliveryData.getOrderId())
                .orElseGet(() -> extractOrderId(metadata));
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
        boolean updated = applyAdminOrderData(order, metadata);
        if (updated) {
            orderRepository.save(order);
        }

        OrderStatusMessage statusMessage = buildStatusMessage(order, message, messageType, metadata, "Delivery update received");
        orderStatusWebSocketController.sendOrderNotification(order.getId(), statusMessage);
        deliveryStatusWebSocketController.sendDeliveryStatusUpdate(order.getId(), statusMessage);
    }

    private Map<String, Object> convertDataToMap(Object data) {
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

    private <T> T convertPayload(Object payload, Class<T> targetType) {
        if (payload == null) {
            return null;
        }

        if (targetType.isInstance(payload)) {
            return targetType.cast(payload);
        }

        try {
            return objectMapper.convertValue(payload, targetType);
        } catch (IllegalArgumentException ex) {
            log.warn("Failed to convert Admin WebSocket payload to {}", targetType.getSimpleName(), ex);
            return null;
        }
    }

    private WebSocketMessageDTO.MessageType resolveMessageType(String rawType) {
        if (!StringUtils.hasText(rawType)) {
            return null;
        }

        try {
            return WebSocketMessageDTO.MessageType.valueOf(rawType.toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown Admin WebSocket message type {}", rawType, ex);
            return null;
        }
    }

    private OrderStatusMessage buildStatusMessage(Order order,
                                                  WebSocketMessageDTO<?> message,
                                                  WebSocketMessageDTO.MessageType messageType,
                                                  Map<String, Object> metadata,
                                                  String defaultMessage) {
        return OrderStatusMessage.builder()
                .orderId(order.getId())
                .status(order.getStatus().name())
                .eventType(resolveEventType(messageType, message))
                .title(resolveTitle(message, messageType))
                .message(Optional.ofNullable(message.getMessage()).orElse(defaultMessage))
                .estimatedDeliveryTime(order.getEstimatedDeliveryTime())
                .timestamp(Optional.ofNullable(message.getTimestamp()).orElse(LocalDateTime.now()))
                .metadata(metadata.isEmpty() ? Collections.emptyMap() : metadata)
                .build();
    }

    private String resolveEventType(WebSocketMessageDTO.MessageType messageType, WebSocketMessageDTO<?> message) {
        if (messageType != null) {
            return messageType.name();
        }
        return Optional.ofNullable(message.getType()).orElse("ORDER_UPDATE");
    }

    private String resolveTitle(WebSocketMessageDTO<?> message, WebSocketMessageDTO.MessageType messageType) {
        if (StringUtils.hasText(message.getTitle())) {
            return message.getTitle();
        }

        if (messageType == null) {
            return "Order Update";
        }

        String normalized = messageType.name().toLowerCase().replace('_', ' ');
        return normalized.isEmpty()
                ? "Order Update"
                : Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private class AdminEventFrameHandler implements StompFrameHandler {

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return WebSocketMessageDTO.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            handlePayload((WebSocketMessageDTO<?>) payload);
        }
    }
}
