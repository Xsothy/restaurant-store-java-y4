package com.restaurant.store.mapper;

import com.restaurant.store.dto.response.OrderItemResponse;
import com.restaurant.store.dto.response.OrderResponse;
import com.restaurant.store.entity.Delivery;
import com.restaurant.store.entity.Order;
import com.restaurant.store.entity.OrderItem;
import com.restaurant.store.entity.Payment;
import com.restaurant.store.entity.Pickup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    @Autowired
    private OrderItemMapper orderItemMapper;

    public OrderResponse toResponse(Order order, List<OrderItem> orderItems) {
        return toResponse(order, orderItems, null, null, null);
    }

    public OrderResponse toResponse(Order order,
                                    List<OrderItem> orderItems,
                                    List<Payment> payments) {
        return toResponse(order, orderItems, payments, null, null);
    }

    public OrderResponse toResponse(Order order,
                                    List<OrderItem> orderItems,
                                    List<Payment> payments,
                                    Delivery delivery,
                                    Pickup pickup) {
        if (order == null) {
            return null;
        }

        List<OrderItemResponse> itemResponses = null;
        if (orderItems != null) {
            itemResponses = orderItems.stream()
                    .map(orderItemMapper::toResponse)
                    .collect(Collectors.toList());
        }
        
        OrderResponse.OrderResponseBuilder builder = OrderResponse.builder()
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
                .orderItems(itemResponses);

        applyLatestPayment(builder, order, payments);
        applyDeliveryDetails(builder, delivery);
        applyPickupDetails(builder, pickup);

        return builder.build();
    }

    private void applyLatestPayment(OrderResponse.OrderResponseBuilder builder,
                                    Order order,
                                    List<Payment> suppliedPayments) {
        List<Payment> payments = suppliedPayments;
        if ((payments == null || payments.isEmpty()) && order != null) {
            payments = order.getPayments();
        }

        if (payments == null || payments.isEmpty()) {
            return;
        }

        Payment latestPayment = payments.stream()
                .filter(Objects::nonNull)
                .max(Comparator.comparing(Payment::getUpdatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);

        if (latestPayment == null) {
            return;
        }

        builder.paymentStatus(latestPayment.getStatus())
                .paymentMethod(latestPayment.getMethod())
                .paymentPaidAt(latestPayment.getPaidAt())
                .paymentTransactionId(latestPayment.getTransactionId());
    }

    private void applyDeliveryDetails(OrderResponse.OrderResponseBuilder builder, Delivery delivery) {
        if (delivery == null) {
            return;
        }

        builder.deliveryStatus(delivery.getStatus())
                .deliveryDriverName(delivery.getDriverName())
                .deliveryDriverPhone(delivery.getDriverPhone())
                .deliveryEstimatedArrivalTime(delivery.getEstimatedArrivalTime())
                .deliveryActualDeliveryTime(delivery.getActualDeliveryTime());
    }

    private void applyPickupDetails(OrderResponse.OrderResponseBuilder builder, Pickup pickup) {
        if (pickup == null) {
            return;
        }

        builder.pickupStatus(pickup.getStatus())
                .pickupCode(pickup.getPickupCode())
                .pickupReadyAt(pickup.getReadyAt())
                .pickupWindowStart(pickup.getWindowStart())
                .pickupWindowEnd(pickup.getWindowEnd())
                .pickupPickedUpAt(pickup.getPickedUpAt())
                .pickupInstructions(pickup.getInstructions());
    }
}
