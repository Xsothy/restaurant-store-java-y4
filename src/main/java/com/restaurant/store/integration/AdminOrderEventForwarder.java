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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminOrderEventForwarder {

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

    public void forwardOrderUpdate(OrderDTO orderData, WebSocketMessageDTO<?> envelope, String defaultMessage) {
        if (orderData == null && (envelope == null || envelope.getData() == null)) {
            log.debug("Skipping Admin order event - no payload provided");
            return;
        }

        WebSocketMessageDTO.MessageType messageType = resolveMessageType(envelope != null ? envelope.getType() : null);
        if (messageType != null && !ORDER_MESSAGE_TYPES.contains(messageType)) {
            log.debug("Skipping Admin order event with non-order type {}", messageType);
        }

        OrderDTO payload = orderData != null
                ? orderData
                : convertPayload(envelope != null ? envelope.getData() : null, OrderDTO.class);

        if (payload == null) {
            log.debug("Skipping Admin order event - payload could not be converted");
            return;
        }

        Map<String, Object> metadata = convertDataToMap(payload);
        Long adminOrderId = Optional.ofNullable(payload.getId())
                .orElseGet(() -> extractOrderId(metadata));
        if (adminOrderId == null) {
            log.debug("Ignoring Admin order event without order id: {}", metadata);
            return;
        }

        Optional<Order> optionalOrder;
        try {
            optionalOrder = orderRepository.findByExternalId(adminOrderId);
            if (optionalOrder.isEmpty()) {
                optionalOrder = orderRepository.findById(adminOrderId);
            }
        } catch (Exception e) {
            log.warn("Error finding order by external_id {}: {}. Trying to find by ID.", adminOrderId, e.getMessage());
            optionalOrder = orderRepository.findById(adminOrderId);
        }

        if (optionalOrder.isEmpty()) {
            log.warn("Received Admin order event for unknown order id {}", adminOrderId);
            return;
        }

        Order order = optionalOrder.get();
        boolean updated = applyAdminOrderData(order, metadata);
        if (updated) {
            orderRepository.save(order);
        }

        OrderStatusMessage statusMessage = buildStatusMessage(order,
                envelope,
                messageType,
                metadata,
                defaultMessage != null ? defaultMessage : "Order update received");
        log.info("Forwarding Admin order event type={} for order {} status {}", messageType,
                order.getId(), order.getStatus());
        orderStatusWebSocketController.sendOrderStatusUpdate(order.getId(), statusMessage);
        orderStatusWebSocketController.sendOrderNotification(order.getId(), statusMessage);
    }

    public void forwardDeliveryUpdate(DeliveryDTO deliveryData, WebSocketMessageDTO<?> envelope, String defaultMessage) {
        if (deliveryData == null && (envelope == null || envelope.getData() == null)) {
            log.debug("Skipping Admin delivery event - no payload provided");
            return;
        }

        WebSocketMessageDTO.MessageType messageType = resolveMessageType(envelope != null ? envelope.getType() : null);
        if (messageType != null && !DELIVERY_MESSAGE_TYPES.contains(messageType)) {
            log.debug("Skipping Admin delivery event with non-delivery type {}", messageType);
        }

        DeliveryDTO payload = deliveryData != null
                ? deliveryData
                : convertPayload(envelope != null ? envelope.getData() : null, DeliveryDTO.class);

        if (payload == null) {
            log.debug("Skipping Admin delivery event - payload could not be converted");
            return;
        }

        Map<String, Object> metadata = convertDataToMap(payload);
        Long adminOrderId = Optional.ofNullable(payload.getOrderId())
                .orElseGet(() -> extractOrderId(metadata));
        if (adminOrderId == null) {
            log.debug("Ignoring Admin delivery event without order id: {}", metadata);
            return;
        }

        Optional<Order> optionalOrder;
        try {
            optionalOrder = orderRepository.findByExternalId(adminOrderId);
            if (optionalOrder.isEmpty()) {
                optionalOrder = orderRepository.findById(adminOrderId);
            }
        } catch (Exception e) {
            log.warn("Error finding order by external_id {}: {}. Trying to find by ID.", adminOrderId, e.getMessage());
            optionalOrder = orderRepository.findById(adminOrderId);
        }

        if (optionalOrder.isEmpty()) {
            log.warn("Received Admin delivery event for unknown order id {}", adminOrderId);
            return;
        }

        Order order = optionalOrder.get();
        boolean updated = applyAdminOrderData(order, metadata);
        if (updated) {
            orderRepository.save(order);
        }

        OrderStatusMessage statusMessage = buildStatusMessage(order,
                envelope,
                messageType,
                metadata,
                defaultMessage != null ? defaultMessage : "Delivery update received");
        log.info("Forwarding Admin delivery event type={} for order {} status {}", messageType,
                order.getId(), order.getStatus());
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
            log.warn("Failed to convert Admin payload data: {}", data, e);
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
            log.warn("Failed to convert Admin payload to {}", targetType.getSimpleName(), ex);
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
                .message(resolveMessage(message, defaultMessage))
                .estimatedDeliveryTime(order.getEstimatedDeliveryTime())
                .timestamp(Optional.ofNullable(message != null ? message.getTimestamp() : null)
                        .orElse(LocalDateTime.now()))
                .metadata(metadata.isEmpty() ? Collections.emptyMap() : metadata)
                .build();
    }

    private String resolveEventType(WebSocketMessageDTO.MessageType messageType, WebSocketMessageDTO<?> message) {
        if (messageType != null) {
            return messageType.name();
        }
        return message != null && StringUtils.hasText(message.getType())
                ? message.getType()
                : "ORDER_UPDATE";
    }

    private String resolveTitle(WebSocketMessageDTO<?> message, WebSocketMessageDTO.MessageType messageType) {
        if (message != null && StringUtils.hasText(message.getTitle())) {
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

    private String resolveMessage(WebSocketMessageDTO<?> message, String defaultMessage) {
        if (message != null && StringUtils.hasText(message.getMessage())) {
            return message.getMessage();
        }

        return StringUtils.hasText(defaultMessage) ? defaultMessage : "Order update received";
    }

    private WebSocketMessageDTO.MessageType resolveMessageType(String rawType) {
        if (!StringUtils.hasText(rawType)) {
            return null;
        }

        try {
            return WebSocketMessageDTO.MessageType.valueOf(rawType.toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown Admin message type {}", rawType, ex);
            return null;
        }
    }
}
