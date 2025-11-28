package com.restaurant.store.dto.response;

import com.restaurant.store.entity.DeliveryStatus;
import com.restaurant.store.entity.OrderStatus;
import com.restaurant.store.entity.OrderType;
import com.restaurant.store.entity.PaymentMethod;
import com.restaurant.store.entity.PaymentStatus;
import com.restaurant.store.entity.PickupStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    
    private Long id;
    private Long customerId;
    private String customerName;
    private OrderStatus status;
    private BigDecimal totalPrice;
    private OrderType orderType;
    private String deliveryAddress;
    private String phoneNumber;
    private String specialInstructions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime estimatedDeliveryTime;
    private List<OrderItemResponse> orderItems;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private LocalDateTime paymentPaidAt;
    private String paymentTransactionId;
    private DeliveryStatus deliveryStatus;
    private String deliveryDriverName;
    private String deliveryDriverPhone;
    private LocalDateTime deliveryEstimatedArrivalTime;
    private LocalDateTime deliveryActualDeliveryTime;
    private Double deliveryLatitude;
    private Double deliveryLongitude;
    private PickupStatus pickupStatus;
    private String pickupCode;
    private LocalDateTime pickupReadyAt;
    private LocalDateTime pickupWindowStart;
    private LocalDateTime pickupWindowEnd;
    private LocalDateTime pickupPickedUpAt;
    private String pickupInstructions;
}
