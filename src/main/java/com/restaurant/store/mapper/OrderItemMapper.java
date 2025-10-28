package com.restaurant.store.mapper;

import com.restaurant.store.dto.response.OrderItemResponse;
import com.restaurant.store.entity.OrderItem;
import org.springframework.stereotype.Component;

@Component
public class OrderItemMapper {
    
    public OrderItemResponse toResponse(OrderItem orderItem) {
        if (orderItem == null) {
            return null;
        }
        
        return OrderItemResponse.builder()
                .id(orderItem.getId())
                .productId(orderItem.getProduct() != null ? orderItem.getProduct().getId() : null)
                .productName(orderItem.getProduct() != null ? orderItem.getProduct().getName() : null)
                .quantity(orderItem.getQuantity())
                .unitPrice(orderItem.getPrice())
                .totalPrice(orderItem.getPrice() != null ? orderItem.getPrice().multiply(java.math.BigDecimal.valueOf(orderItem.getQuantity())) : null)
                .specialInstructions(orderItem.getSpecialInstructions())
                .build();
    }
}
