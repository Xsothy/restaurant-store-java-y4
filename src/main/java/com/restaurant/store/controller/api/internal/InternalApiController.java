package com.restaurant.store.controller.api.internal;

import com.restaurant.store.controller.api.OrderStatusWebSocketController;
import com.restaurant.store.dto.request.OrderStatusUpdateRequest;
import com.restaurant.store.dto.response.ApiResponse;
import com.restaurant.store.dto.response.OrderStatusMessage;
import com.restaurant.store.entity.Order;
import com.restaurant.store.entity.OrderItem;
import com.restaurant.store.entity.OrderStatus;
import com.restaurant.store.entity.OrderType;
import com.restaurant.store.entity.Payment;
import com.restaurant.store.entity.PaymentMethod;
import com.restaurant.store.entity.PaymentStatus;
import com.restaurant.store.entity.PickupStatus;
import com.restaurant.store.mapper.OrderMapper;
import com.restaurant.store.repository.OrderItemRepository;
import com.restaurant.store.repository.OrderRepository;
import com.restaurant.store.repository.PaymentRepository;
import com.restaurant.store.repository.PickupRepository;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
@Slf4j
@Hidden
public class InternalApiController {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderMapper orderMapper;
    private final OrderStatusWebSocketController webSocketController;
    private final PaymentRepository paymentRepository;
    private final PickupRepository pickupRepository;

    @PostMapping("/orders/{orderId}/status")
    public ResponseEntity<ApiResponse<Object>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody OrderStatusUpdateRequest request) {
        
        log.info("Received order status update for order: {} - New status: {}", orderId, request.getStatus());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        OrderStatus newStatus = OrderStatus.valueOf(request.getStatus());
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());

        if (request.getEstimatedDeliveryTime() != null) {
            order.setEstimatedDeliveryTime(request.getEstimatedDeliveryTime());
        }

        orderRepository.save(order);
        syncPickupStatus(order, newStatus);

        if (newStatus == OrderStatus.COMPLETED) {
            markCashOnDeliveryPaymentsAsCompleted(orderId);
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        webSocketController.sendOrderUpdate(orderId, orderMapper.toResponse(order, orderItems));

        OrderStatusMessage statusMessage = OrderStatusMessage.builder()
                .status(order.getStatus().name())
                .eventType("ORDER_STATUS_CHANGED")
                .title("Order status updated")
                .message("Your order is now " + newStatus.name())
                .estimatedDeliveryTime(order.getEstimatedDeliveryTime())
                .metadata(buildMetadata(order, request))
                .build();
        webSocketController.sendOrderStatusUpdate(orderId, statusMessage);
        webSocketController.sendOrderNotification(orderId, statusMessage);

        return ResponseEntity.ok(ApiResponse.success(
                "Order status updated successfully",
                orderMapper.toResponse(order, orderItems)
        ));
    }

    private void markCashOnDeliveryPaymentsAsCompleted(Long orderId) {
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        if (payments.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        payments.stream()
                .filter(payment -> payment.getMethod() == PaymentMethod.CASH_ON_DELIVERY)
                .filter(payment -> payment.getStatus() != PaymentStatus.COMPLETED)
                .forEach(payment -> {
                    payment.setStatus(PaymentStatus.COMPLETED);
                    if (payment.getPaidAt() == null) {
                        payment.setPaidAt(now);
                    }
                    payment.setUpdatedAt(now);
                    paymentRepository.save(payment);
                });
    }

    @PostMapping("/orders/{orderId}/sync")
    public ResponseEntity<ApiResponse<Object>> syncOrderFromAdmin(
            @PathVariable Long orderId,
            @RequestParam Long externalId) {
        
        log.info("Syncing order {} with external ID: {}", orderId, externalId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        order.setExternalId(externalId);
        order.setSyncedAt(LocalDateTime.now());
        orderRepository.save(order);

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success(
                "Order synced successfully",
                orderMapper.toResponse(order, orderItems)
        ));
    }

    private void syncPickupStatus(Order order, OrderStatus newStatus) {
        if (order.getOrderType() != OrderType.PICKUP) {
            return;
        }

        pickupRepository.findByOrderId(order.getId()).ifPresent(pickup -> {
            switch (newStatus) {
                case PREPARING -> pickup.setStatus(PickupStatus.PREPARING);
                case READY_FOR_DELIVERY -> pickup.setStatus(PickupStatus.READY_FOR_PICKUP);
                case COMPLETED -> {
                    pickup.setStatus(PickupStatus.COMPLETED);
                    pickup.setPickedUpAt(LocalDateTime.now());
                }
                case CANCELLED -> pickup.setStatus(PickupStatus.CANCELLED);
                default -> { }
            }

            if (pickup.getStatus() == PickupStatus.READY_FOR_PICKUP) {
                LocalDateTime now = LocalDateTime.now();
                pickup.setReadyAt(now);
                pickup.setWindowStart(now.minusMinutes(5));
                pickup.setWindowEnd(now.plusHours(1));
            }

            pickupRepository.save(pickup);
        });
    }

    private Map<String, Object> buildMetadata(Order order, OrderStatusUpdateRequest request) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("orderId", order.getId());
        metadata.put("orderType", order.getOrderType().name());
        if (request.getEstimatedDeliveryTime() != null) {
            metadata.put("estimatedDeliveryTime", request.getEstimatedDeliveryTime());
        }

        if (order.getOrderType() == OrderType.PICKUP) {
            pickupRepository.findByOrderId(order.getId()).ifPresent(pickup -> {
                metadata.put("pickupStatus", pickup.getStatus().name());
                metadata.put("pickupCode", pickup.getPickupCode());
                metadata.put("pickupWindowStart", pickup.getWindowStart());
                metadata.put("pickupWindowEnd", pickup.getWindowEnd());
            });
        }

        return metadata;
    }
}
