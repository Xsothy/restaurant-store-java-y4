package com.restaurant.store.integration;

import com.restaurant.store.dto.admin.OrderDTO;
import com.restaurant.store.dto.admin.websocket.WebSocketMessageDTO;
import com.restaurant.store.entity.OrderStatus;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "admin.api.order-status.polling.enabled", havingValue = "true")
public class AdminOrderStatusPollingService {

    private final AdminIntegrationService adminIntegrationService;
    private final AdminOrderEventForwarder adminOrderEventForwarder;

    @Value("${admin.api.order-status.polling.statuses:PENDING,CONFIRMED,PREPARING,READY_FOR_PICKUP,READY_FOR_DELIVERY,OUT_FOR_DELIVERY,COMPLETED,CANCELLED}")
    private String polledStatuses;

    @Value("${admin.api.order-status.polling.interval:2000}")
    private long pollingIntervalMs;

    @PostConstruct
    public void logPollingMode() {
        log.info("Admin order status polling mode enabled (interval={} ms, statuses={}). WebSocket bridge remains disabled while this flag is true.",
                pollingIntervalMs,
                polledStatuses);
    }

    @Scheduled(fixedDelayString = "${admin.api.order-status.polling.interval:2000}")
    public void pollOrderStatuses() {
        List<OrderStatus> statusesToPoll = parseStatuses();
        if (statusesToPoll.isEmpty()) {
            log.debug("Order status polling skipped - no statuses configured");
            return;
        }

        statusesToPoll.forEach(status -> {
            List<OrderDTO> remoteOrders = adminIntegrationService.fetchOrdersByStatus(status);
            log.info("Polled {} admin orders for status {}", remoteOrders.size(), status);
            remoteOrders.forEach(order -> forwardPolledOrder(order, status));
        });
    }

    private void forwardPolledOrder(OrderDTO order, OrderStatus requestedStatus) {
        if (order == null || order.getId() == null) {
            log.debug("Skipping polled order for status {} due to missing payload", requestedStatus);
            return;
        }

        WebSocketMessageDTO<OrderDTO> message = WebSocketMessageDTO.<OrderDTO>builder()
                .type(WebSocketMessageDTO.MessageType.ORDER_STATUS_CHANGED.name())
                .title("Admin status poll")
                .message(String.format("Polled status %s from Admin API", order.getStatus()))
                .timestamp(LocalDateTime.now())
                .data(order)
                .build();

        adminOrderEventForwarder.forwardOrderUpdate(order, message, "Polled order update received");
    }

    private List<OrderStatus> parseStatuses() {
        if (!StringUtils.hasText(polledStatuses)) {
            return List.of();
        }

        return Arrays.stream(polledStatuses.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(value -> {
                    try {
                        return OrderStatus.valueOf(value.toUpperCase(Locale.US));
                    } catch (IllegalArgumentException ex) {
                        log.warn("Ignoring unknown order status '{}' in polling configuration", value);
                        return null;
                    }
                })
                .filter(status -> status != null)
                .collect(Collectors.toList());
    }
}
