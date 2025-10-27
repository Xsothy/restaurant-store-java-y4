package com.restaurant.store.dto.request;

import com.restaurant.store.entity.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public class PaymentRequest {
    
    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
    
    private String paymentToken;
    
    private String cardLastFourDigits;
    
    // Constructors
    public PaymentRequest() {
    }
    
    public PaymentRequest(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public PaymentRequest(PaymentMethod paymentMethod, String paymentToken) {
        this.paymentMethod = paymentMethod;
        this.paymentToken = paymentToken;
    }
    
    // Getters and Setters
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public String getPaymentToken() {
        return paymentToken;
    }
    
    public void setPaymentToken(String paymentToken) {
        this.paymentToken = paymentToken;
    }
    
    public String getCardLastFourDigits() {
        return cardLastFourDigits;
    }
    
    public void setCardLastFourDigits(String cardLastFourDigits) {
        this.cardLastFourDigits = cardLastFourDigits;
    }
}