package com.restaurant.store.dto.response;

import com.restaurant.store.entity.DeliveryStatus;

import java.time.LocalDateTime;

public class DeliveryResponse {
    
    private Long id;
    private Long orderId;
    private String driverName;
    private String driverPhone;
    private String vehicleInfo;
    private DeliveryStatus status;
    private LocalDateTime pickupTime;
    private LocalDateTime estimatedArrivalTime;
    private LocalDateTime actualDeliveryTime;
    private String deliveryNotes;
    private String currentLocation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public DeliveryResponse() {
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getOrderId() {
        return orderId;
    }
    
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    
    public String getDriverName() {
        return driverName;
    }
    
    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }
    
    public String getDriverPhone() {
        return driverPhone;
    }
    
    public void setDriverPhone(String driverPhone) {
        this.driverPhone = driverPhone;
    }
    
    public String getVehicleInfo() {
        return vehicleInfo;
    }
    
    public void setVehicleInfo(String vehicleInfo) {
        this.vehicleInfo = vehicleInfo;
    }
    
    public DeliveryStatus getStatus() {
        return status;
    }
    
    public void setStatus(DeliveryStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getPickupTime() {
        return pickupTime;
    }
    
    public void setPickupTime(LocalDateTime pickupTime) {
        this.pickupTime = pickupTime;
    }
    
    public LocalDateTime getEstimatedArrivalTime() {
        return estimatedArrivalTime;
    }
    
    public void setEstimatedArrivalTime(LocalDateTime estimatedArrivalTime) {
        this.estimatedArrivalTime = estimatedArrivalTime;
    }
    
    public LocalDateTime getActualDeliveryTime() {
        return actualDeliveryTime;
    }
    
    public void setActualDeliveryTime(LocalDateTime actualDeliveryTime) {
        this.actualDeliveryTime = actualDeliveryTime;
    }
    
    public String getDeliveryNotes() {
        return deliveryNotes;
    }
    
    public void setDeliveryNotes(String deliveryNotes) {
        this.deliveryNotes = deliveryNotes;
    }
    
    public String getCurrentLocation() {
        return currentLocation;
    }
    
    public void setCurrentLocation(String currentLocation) {
        this.currentLocation = currentLocation;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}