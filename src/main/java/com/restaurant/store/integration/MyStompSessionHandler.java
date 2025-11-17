package com.restaurant.store.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.store.dto.admin.DeliveryDTO;
import com.restaurant.store.dto.admin.OrderDTO;
import com.restaurant.store.dto.admin.websocket.WebSocketMessageDTO;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class MyStompSessionHandler extends StompSessionHandlerAdapter {

    private final AdminOrderEventForwarder adminOrderEventForwarder;
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

    public MyStompSessionHandler(AdminOrderEventForwarder adminOrderEventForwarder,
                                 ObjectMapper objectMapper,
                                 WebSocketStompClient stompClient,
                                 String websocketUrl,
                                 String subscriptionTopic,
                                 String deliverySubscriptionTopic) {
        this.adminOrderEventForwarder = adminOrderEventForwarder;
        this.objectMapper = objectMapper;
        this.stompClient = stompClient;
        this.websocketUrl = websocketUrl;
        this.subscriptionTopic = subscriptionTopic;
        this.deliverySubscriptionTopic = deliverySubscriptionTopic;
    }

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        log.info("Admin WebSocket connected: session={}, headers={}", session.getSessionId(), connectedHeaders);
        reconnectAttempts.set(0);
        this.session = session;
        session.subscribe(subscriptionTopic, adminFrameHandler);
        log.info("Subscribed to admin order topic: {}", subscriptionTopic);
        session.subscribe(deliverySubscriptionTopic, adminFrameHandler);
        log.info("Subscribed to admin delivery topic: {}", deliverySubscriptionTopic);
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
            log.info("Attempting Admin WebSocket reconnect #{} to {}", reconnectAttempts.get(), websocketUrl);
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
            log.info("Received Admin WebSocket message type={} timestamp={}", message.getType(), message.getTimestamp());
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

        switch (messageType) {
            case ORDER_CREATED, ORDER_UPDATED, ORDER_STATUS_CHANGED -> handleOrderMessage(message);
            case DELIVERY_ASSIGNED, DELIVERY_STATUS_UPDATED -> handleDeliveryMessage(message);
            default -> log.debug("Admin WebSocket message type {} does not require forwarding", messageType);
        }
    }

    private void handleOrderMessage(WebSocketMessageDTO<?> message) {
        OrderDTO orderData = convertPayload(message.getData(), OrderDTO.class);
        if (orderData == null) {
            log.debug("Admin order message payload could not be converted: {}", message.getData());
            return;
        }

        adminOrderEventForwarder.forwardOrderUpdate(orderData, message, "Order update received");
    }

    private void handleDeliveryMessage(WebSocketMessageDTO<?> message) {
        DeliveryDTO deliveryData = convertPayload(message.getData(), DeliveryDTO.class);
        if (deliveryData == null) {
            log.debug("Admin delivery message payload could not be converted: {}", message.getData());
            return;
        }

        adminOrderEventForwarder.forwardDeliveryUpdate(deliveryData, message, "Delivery update received");
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

    private class AdminEventFrameHandler implements StompFrameHandler {

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return WebSocketMessageDTO.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            log.debug("Admin frame received headers={}", headers);
            handlePayload((WebSocketMessageDTO<?>) payload);
        }
    }
}
