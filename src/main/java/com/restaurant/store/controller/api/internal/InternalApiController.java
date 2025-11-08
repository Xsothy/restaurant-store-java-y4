package com.restaurant.store.controller.api.internal;

import com.restaurant.store.controller.api.OrderStatusWebSocketController;
import com.restaurant.store.dto.request.OrderStatusUpdateRequest;
import com.restaurant.store.dto.response.ApiResponse;
import com.restaurant.store.entity.Order;
import com.restaurant.store.entity.OrderItem;
import com.restaurant.store.entity.OrderStatus;
import com.restaurant.store.mapper.OrderMapper;
import com.restaurant.store.repository.OrderItemRepository;
import com.restaurant.store.repository.OrderRepository;
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

    @PostMapping("/orders/{orderId}/status")
    public ResponseEntity<ApiResponse<Object>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody OrderStatusUpdateRequest request) {
        
        log.info("Received order status update for order: {} - New status: {}", orderId, request.getStatus());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        order.setStatus(OrderStatus.valueOf(request.getStatus()));
        order.setUpdatedAt(LocalDateTime.now());

        if (request.getEstimatedDeliveryTime() != null) {
            order.setEstimatedDeliveryTime(request.getEstimatedDeliveryTime());
        }

        orderRepository.save(order);

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        webSocketController.sendOrderUpdate(orderId, orderMapper.toResponse(order, orderItems));

        return ResponseEntity.ok(ApiResponse.success(
                "Order status updated successfully",
                orderMapper.toResponse(order, orderItems)
        ));
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
