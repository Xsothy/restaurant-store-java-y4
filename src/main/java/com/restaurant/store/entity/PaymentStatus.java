package com.restaurant.store.entity;

public enum PaymentStatus {
    PENDING,
    AWAITING_SESSION,
    AWAITING_WEBHOOK,
    CASH_PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED,
    REFUNDED
}