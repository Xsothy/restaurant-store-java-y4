package com.restaurant.store.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "deliveries")
public class Delivery {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "Order is required")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @NotBlank(message = "Driver name is required")
    @Size(max = 100)
    @Column(name = "driver_name", nullable = false, length = 100)
    private String driverName;
    
    @Size(max = 20)
    @Column(name = "driver_phone", length = 20)
    private String driverPhone;
    
    @Column(name = "vehicle_info")
    private String vehicleInfo;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status = DeliveryStatus.ASSIGNED;
    
    @Column(name = "pickup_time")
    private LocalDateTime pickupTime;
    
    @Column(name = "estimated_arrival_time")
    private LocalDateTime estimatedArrivalTime;
    
    @Column(name = "actual_delivery_time")
    private LocalDateTime actualDeliveryTime;
    
    @Column(name = "delivery_notes")
    private String deliveryNotes;
    
    @Column(name = "current_location")
    private String currentLocation;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructors
    public Delivery() {
    }
    
    public Delivery(Order order, String driverName, String driverPhone, String vehicleInfo) {
        this.order = order;
        this.driverName = driverName;
        this.driverPhone = driverPhone;
        this.vehicleInfo = vehicleInfo;
        this.status = DeliveryStatus.ASSIGNED;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = DeliveryStatus.ASSIGNED;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (status == DeliveryStatus.DELIVERED && actualDeliveryTime == null) {
            actualDeliveryTime = LocalDateTime.now();
        }
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Order getOrder() {
        return order;
    }
    
    public void setOrder(Order order) {
        this.order = order;
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