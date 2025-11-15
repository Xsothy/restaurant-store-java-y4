package com.restaurant.store.controller.api.internal;

import com.restaurant.store.controller.api.OrderStatusWebSocketController;
import com.restaurant.store.dto.request.OrderStatusUpdateRequest;
import com.restaurant.store.dto.response.ApiResponse;
import com.restaurant.store.entity.Order;
import com.restaurant.store.entity.OrderItem;
import com.restaurant.store.entity.OrderStatus;
import com.restaurant.store.entity.Payment;
import com.restaurant.store.entity.PaymentMethod;
import com.restaurant.store.entity.PaymentStatus;
import com.restaurant.store.mapper.OrderMapper;
import com.restaurant.store.repository.OrderItemRepository;
import com.restaurant.store.repository.OrderRepository;
import com.restaurant.store.repository.PaymentRepository;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

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

        if (newStatus == OrderStatus.DELIVERED) {
            markCashOnDeliveryPaymentsAsCompleted(orderId);
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        webSocketController.sendOrderUpdate(orderId, orderMapper.toResponse(order, orderItems));

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
}
