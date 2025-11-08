package com.restaurant.store.service;

import com.restaurant.store.dto.response.PaymentResponse;
import com.restaurant.store.entity.Order;
import com.stripe.exception.StripeException;

public interface PaymentService {
    
    PaymentResponse createPayment(Order order) throws StripeException;
    
    void handlePaymentSuccess(String paymentId) throws StripeException;
    
    void handlePaymentCancel(String paymentId) throws StripeException;
    
    String getServiceType();
}
