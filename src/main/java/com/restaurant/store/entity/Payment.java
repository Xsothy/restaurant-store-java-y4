package com.restaurant.store.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotNull(message = "Order is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than 0")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;
    
    @Column(name = "transaction_id")
    private String transactionId;
    
    @Column(name = "payment_gateway_response")
    private String paymentGatewayResponse;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public Payment(Order order, BigDecimal amount, PaymentMethod method) {
        this.order = order;
        this.amount = amount;
        this.method = method;
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = PaymentStatus.PENDING;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (status == PaymentStatus.COMPLETED && paidAt == null) {
            paidAt = LocalDateTime.now();
        }
    }
}