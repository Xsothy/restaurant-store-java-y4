package com.restaurant.store.controller.api.internal;

import com.restaurant.store.controller.api.OrderStatusWebSocketController;
import com.restaurant.store.dto.request.OrderStatusUpdateRequest;
import com.restaurant.store.dto.response.ApiResponse;
import com.restaurant.store.dto.response.DeliveryResponse;
import com.restaurant.store.dto.response.OrderStatusMessage;
import com.restaurant.store.entity.DeliveryStatus;
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
import com.restaurant.store.service.DeliveryService;
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
    private final DeliveryService deliveryService;

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

        if (newStatus == OrderStatus.DELIVERED) {
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
                case READY -> pickup.setStatus(PickupStatus.READY_FOR_PICKUP);
                case DELIVERED -> {
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

    @PostMapping("/deliveries/{orderId}/status")
    public ResponseEntity<ApiResponse<DeliveryResponse>> updateDeliveryStatus(
            @PathVariable Long orderId,
            @RequestParam String status,
            @RequestParam(required = false) String location) {

        log.info("Received delivery status update for order: {} - New status: {}, Location: {}", 
                orderId, status, location);

        DeliveryStatus deliveryStatus = DeliveryStatus.valueOf(status);
        DeliveryResponse response = deliveryService.updateDeliveryStatus(orderId, deliveryStatus, location);

        return ResponseEntity.ok(ApiResponse.success(
                "Delivery status updated successfully",
                response
        ));
    }

    @PostMapping("/deliveries/{orderId}/location")
    public ResponseEntity<ApiResponse<String>> updateDeliveryLocation(
            @PathVariable Long orderId,
            @RequestParam String location) {

        log.info("Received delivery location update for order: {} - Location: {}", orderId, location);

        deliveryService.updateDeliveryLocation(orderId, location);

        return ResponseEntity.ok(ApiResponse.success(
                "Delivery location updated successfully",
                "Location updated to: " + location
        ));
    }

    @PostMapping("/deliveries/{orderId}/driver")
    public ResponseEntity<ApiResponse<DeliveryResponse>> assignDriver(
            @PathVariable Long orderId,
            @RequestParam String driverName,
            @RequestParam String driverPhone,
            @RequestParam(required = false) String vehicleInfo) {

        log.info("Assigning driver to delivery for order: {} - Driver: {}", orderId, driverName);

        DeliveryResponse response = deliveryService.assignDriver(orderId, driverName, driverPhone, vehicleInfo);

        return ResponseEntity.ok(ApiResponse.success(
                "Driver assigned successfully",
                response
        ));
    }
}
