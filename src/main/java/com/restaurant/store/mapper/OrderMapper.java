package com.restaurant.store.mapper;

import com.restaurant.store.dto.response.OrderItemResponse;
import com.restaurant.store.dto.response.OrderResponse;
import com.restaurant.store.entity.Order;
import com.restaurant.store.entity.OrderItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {
    
    @Autowired
    private OrderItemMapper orderItemMapper;
    
    public OrderResponse toResponse(Order order, List<OrderItem> orderItems) {
        if (order == null) {
            return null;
        }
        
        List<OrderItemResponse> itemResponses = null;
        if (orderItems != null) {
            itemResponses = orderItems.stream()
                    .map(orderItemMapper::toResponse)
                    .collect(Collectors.toList());
        }
        
        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomer() != null ? order.getCustomer().getId() : null)
                .customerName(order.getCustomer() != null ? order.getCustomer().getName() : null)
                .status(order.getStatus())
                .totalPrice(order.getTotalPrice())
                .orderType(order.getOrderType())
                .deliveryAddress(order.getDeliveryAddress())
                .phoneNumber(order.getPhoneNumber())
                .specialInstructions(order.getSpecialInstructions())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .estimatedDeliveryTime(order.getEstimatedDeliveryTime())
                .orderItems(itemResponses)
                .build();
    }
}
