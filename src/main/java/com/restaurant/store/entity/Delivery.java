package com.restaurant.store.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "deliveries")
@Getter
@Setter
@NoArgsConstructor
public class Delivery {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    
    @NotNull(message = "Order is required")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @Column(name = "delivery_address")
    private String deliveryAddress;
    
    @Column(name = "phone_number")
    private String phoneNumber;
    
    @Size(max = 100)
    @Column(name = "driver_name", length = 100)
    private String driverName;
    
    @Size(max = 20)
    @Column(name = "driver_phone", length = 20)
    private String driverPhone;
    
    @Column(name = "vehicle_info")
    private String vehicleInfo;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status = DeliveryStatus.PENDING;
    
    @Column(name = "pickup_time")
    private LocalDateTime pickupTime;
    
    @Column(name = "estimated_arrival_time")
    private LocalDateTime estimatedArrivalTime;
    
    @Column(name = "estimated_delivery_time")
    private LocalDateTime estimatedDeliveryTime;
    
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
    
    public Delivery(Order order, String driverName, String driverPhone, String vehicleInfo) {
        this.order = order;
        this.driverName = driverName;
        this.driverPhone = driverPhone;
        this.vehicleInfo = vehicleInfo;
        this.status = DeliveryStatus.ASSIGNED;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
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
}