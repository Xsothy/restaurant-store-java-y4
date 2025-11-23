package com.restaurant.store.integration;

import com.restaurant.store.dto.admin.DeliveryDTO;
import com.restaurant.store.dto.admin.OrderDTO;
import com.restaurant.store.dto.admin.websocket.WebSocketMessageDTO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "admin.api.order-status.polling.enabled", havingValue = "true")
public class AdminOrderStatusPollingService {

    private final AdminIntegrationService adminIntegrationService;
    private final AdminOrderEventForwarder adminOrderEventForwarder;

    @Value("${admin.api.order-status.polling.interval:2000}")
    private long pollingIntervalMs;

    @PostConstruct
    public void logPollingMode() {
        log.info("Admin order status polling mode enabled (interval={} ms). WebSocket bridge remains disabled while this flag is true.",
                pollingIntervalMs);
    }

    @Scheduled(fixedDelayString = "${admin.api.order-status.polling.interval:2000}")
    public void pollOrderStatuses() {
        pollKitchenOrders();
        pollDeliveryOrders();
    }

    private void pollKitchenOrders() {
        List<OrderDTO> remoteOrders = adminIntegrationService.fetchKitchenOrders();
        log.info("Polled {} admin kitchen orders", remoteOrders.size());
        remoteOrders.forEach(order -> forwardPolledOrder(order, "kitchen"));
    }

    private void pollDeliveryOrders() {
        List<OrderDTO> remoteOrders = adminIntegrationService.fetchDeliveryOrders();
        log.info("Polled {} admin delivery orders", remoteOrders.size());
        remoteOrders.forEach(this::forwardPolledDeliveryOrder);
    }

    private void forwardPolledOrder(OrderDTO order, String source) {
        if (order == null || order.getId() == null) {
            log.debug("Skipping polled {} order due to missing payload", source);
            return;
        }

        WebSocketMessageDTO<OrderDTO> message = WebSocketMessageDTO.<OrderDTO>builder()
                .type(WebSocketMessageDTO.MessageType.ORDER_STATUS_CHANGED.name())
                .title("Admin status poll")
                .message(String.format("Polled %s order status %s from Admin API", source, order.getStatus()))
                .timestamp(LocalDateTime.now())
                .data(order)
                .build();

        adminOrderEventForwarder.forwardOrderUpdate(order, message, "Polled order update received");
    }

    private void forwardPolledDeliveryOrder(OrderDTO order) {
        forwardPolledOrder(order, "delivery");

        DeliveryDTO delivery = Optional.ofNullable(order).map(OrderDTO::getDelivery).orElse(null);
        if (delivery == null) {
            log.debug("Polled delivery order {} missing delivery payload", order != null ? order.getId() : null);
            return;
        }

        if (delivery.getOrderId() == null && order != null) {
            delivery.setOrderId(order.getId());
        }

        WebSocketMessageDTO<DeliveryDTO> message = WebSocketMessageDTO.<DeliveryDTO>builder()
                .type(WebSocketMessageDTO.MessageType.DELIVERY_STATUS_UPDATED.name())
                .title("Admin delivery poll")
                .message(String.format("Polled delivery status %s from Admin API", delivery.getStatus()))
                .timestamp(LocalDateTime.now())
                .data(delivery)
                .build();

        adminOrderEventForwarder.forwardDeliveryUpdate(delivery, message, "Polled delivery update received");
    }
}
