package com.restaurant.store.service;

import com.restaurant.store.dto.response.PaymentResponse;
import com.restaurant.store.entity.Order;
import com.restaurant.store.entity.Payment;
import com.restaurant.store.entity.PaymentMethod;
import com.restaurant.store.entity.PaymentStatus;
import com.restaurant.store.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "payment.service.type", havingValue = "intent", matchIfMissing = true)
public class PaymentIntentService implements PaymentService {

    private final PaymentRepository paymentRepository;
    
    @Value("${stripe.secret.key}")
    private String stripeSecretKey;
    
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
        log.info("PaymentIntentService initialized");
    }

    @Override
    @Transactional
    public PaymentResponse createPayment(Order order) throws StripeException {
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

        Payment payment = paymentRepository.findByOrderIdAndStatus(order.getId(), PaymentStatus.AWAITING_WEBHOOK)
                .orElseGet(() -> paymentRepository.findByOrderIdAndStatus(order.getId(), PaymentStatus.PENDING)
                        .orElse(new Payment()));

        payment.setOrder(order);
        payment.setAmount(order.getTotalPrice());
        payment.setMethod(PaymentMethod.STRIPE);
        payment.setStatus(PaymentStatus.AWAITING_WEBHOOK);
        payment.setTransactionId(paymentIntent.getId());
        if (payment.getCreatedAt() == null) {
            payment.setCreatedAt(LocalDateTime.now());
        }
        payment.setUpdatedAt(LocalDateTime.now());

        paymentRepository.save(payment);

        log.info("Payment intent created successfully: {}", paymentIntent.getId());
        
        return PaymentResponse.builder()
                .paymentId(paymentIntent.getId())
                .clientSecret(paymentIntent.getClientSecret())
                .type("intent")
                .build();
    }

    @Override
    @Transactional
    public void handlePaymentSuccess(String paymentId) throws StripeException {
        log.info("Handling payment success for intent: {}", paymentId);

        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentId);

        Payment payment = paymentRepository.findByTransactionId(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found for intent: " + paymentId));

        if ("succeeded".equals(paymentIntent.getStatus())) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setPaidAt(LocalDateTime.now());
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
            log.info("Payment confirmed successfully: {}", paymentId);
        } else {
            log.warn("Payment intent status is not succeeded: {}", paymentIntent.getStatus());
        }
    }

    @Override
    @Transactional
    public void handlePaymentCancel(String paymentId) throws StripeException {
        log.info("Handling payment cancel for intent: {}", paymentId);

        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentId);

        Payment payment = paymentRepository.findByTransactionId(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found for intent: " + paymentId));

        payment.setStatus(PaymentStatus.FAILED);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);
        log.info("Payment intent marked as failed: {}", paymentId);
    }

    @Override
    public String getServiceType() {
        return "intent";
    }
}
