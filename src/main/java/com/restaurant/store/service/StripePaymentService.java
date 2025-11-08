package com.restaurant.store.service;

import com.restaurant.store.entity.Order;
import com.restaurant.store.entity.Payment;
import com.restaurant.store.entity.PaymentMethod;
import com.restaurant.store.entity.PaymentStatus;
import com.restaurant.store.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripePaymentService {

    private final PaymentRepository paymentRepository;
    
    @Value("${stripe.secret.key}")
    private String stripeSecretKey;
    
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @Transactional
    public Map<String, Object> createPaymentIntent(Order order) throws StripeException {
        log.info("Creating payment intent for order ID: {}", order.getId());

        long amountInCents = order.getTotalPrice().multiply(BigDecimal.valueOf(100)).longValue();

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency("usd")
                .putMetadata("orderId", String.valueOf(order.getId()))
                .putMetadata("customerId", String.valueOf(order.getCustomer().getId()))
                .setDescription("Order #" + order.getId() + " payment")
                .build();

        PaymentIntent paymentIntent = PaymentIntent.create(params);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotalPrice());
        payment.setMethod(PaymentMethod.STRIPE);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionId(paymentIntent.getId());
        payment.setCreatedAt(LocalDateTime.now());

        paymentRepository.save(payment);

        Map<String, Object> response = new HashMap<>();
        response.put("clientSecret", paymentIntent.getClientSecret());
        response.put("paymentIntentId", paymentIntent.getId());
        response.put("amount", order.getTotalPrice());

        log.info("Payment intent created successfully: {}", paymentIntent.getId());
        return response;
    }

    @Transactional
    public Payment confirmPayment(String paymentIntentId) throws StripeException {
        log.info("Confirming payment for intent: {}", paymentIntentId);

        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

        Payment payment = paymentRepository.findByTransactionId(paymentIntentId)
                .orElseThrow(() -> new RuntimeException("Payment not found for intent: " + paymentIntentId));

        if ("succeeded".equals(paymentIntent.getStatus())) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setPaidAt(LocalDateTime.now());
            log.info("Payment confirmed successfully: {}", paymentIntentId);
        } else if ("canceled".equals(paymentIntent.getStatus())) {
            payment.setStatus(PaymentStatus.FAILED);
            log.warn("Payment was canceled: {}", paymentIntentId);
        } else {
            payment.setStatus(PaymentStatus.PENDING);
            log.info("Payment still pending: {}", paymentIntentId);
        }

        return paymentRepository.save(payment);
    }

    public void handleWebhookEvent(String payload, String sigHeader) {
        log.info("Processing Stripe webhook event");
        // Webhook handling logic would go here
        // This would verify the signature and process the event
        log.info("Webhook event processed");
    }
}
