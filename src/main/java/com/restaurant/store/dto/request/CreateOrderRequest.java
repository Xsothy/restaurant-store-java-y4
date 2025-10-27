package com.restaurant.store.dto.request;

import com.restaurant.store.entity.OrderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class CreateOrderRequest {
    
    @NotEmpty(message = "Order items cannot be empty")
    @Valid
    private List<OrderItemRequest> orderItems;
    
    @NotNull(message = "Order type is required")
    private OrderType orderType;
    
    private String deliveryAddress;
    
    @Size(max = 20, message = "Phone number must be less than 20 characters")
    private String phoneNumber;
    
    @Size(max = 500, message = "Special instructions must be less than 500 characters")
    private String specialInstructions;
    
    // Constructors
    public CreateOrderRequest() {
    }
    
    public CreateOrderRequest(List<OrderItemRequest> orderItems, OrderType orderType, String deliveryAddress, String phoneNumber) {
        this.orderItems = orderItems;
        this.orderType = orderType;
        this.deliveryAddress = deliveryAddress;
        this.phoneNumber = phoneNumber;
    }
    
    // Getters and Setters
    public List<OrderItemRequest> getOrderItems() {
        return orderItems;
    }
    
    public void setOrderItems(List<OrderItemRequest> orderItems) {
        this.orderItems = orderItems;
    }
    
    public OrderType getOrderType() {
        return orderType;
    }
    
    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }
    
    public String getDeliveryAddress() {
        return deliveryAddress;
    }
    
    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getSpecialInstructions() {
        return specialInstructions;
    }
    
    public void setSpecialInstructions(String specialInstructions) {
        this.specialInstructions = specialInstructions;
    }
}