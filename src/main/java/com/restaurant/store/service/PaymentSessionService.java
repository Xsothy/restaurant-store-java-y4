package com.restaurant.store.service;

import com.restaurant.store.dto.response.PaymentResponse;
import com.restaurant.store.entity.Order;
import com.restaurant.store.entity.Payment;
import com.restaurant.store.entity.PaymentMethod;
import com.restaurant.store.entity.PaymentStatus;
import com.restaurant.store.exception.ResourceNotFoundException;
import com.restaurant.store.repository.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
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
@ConditionalOnProperty(name = "payment.service.type", havingValue = "session")
public class PaymentSessionService implements PaymentService {

    private final PaymentRepository paymentRepository;
    
    @Value("${stripe.secret.key}")
    private String stripeSecretKey;
    
    @Value("${payment.success.url:http://localhost:8080/payment/success}")
    private String successUrl;
    
    @Value("${payment.cancel.url:http://localhost:8080/payment/cancel}")
    private String cancelUrl;
    
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
        log.info("PaymentSessionService initialized");
    }

    @Override
    @Transactional
    public PaymentResponse createPayment(Order order) throws StripeException {
        log.info("Creating checkout session for order ID: {}", order.getId());

        long amountInCents = order.getTotalPrice().multiply(BigDecimal.valueOf(100)).longValue();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl + "?session_id={CHECKOUT_SESSION_ID}")
                .addLineItem(
                    SessionCreateParams.LineItem.builder()
                        .setPriceData(
                            SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("usd")
                                .setUnitAmount(amountInCents)
                                .setProductData(
                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Order #" + order.getId())
                                        .setDescription("Restaurant order payment")
                                        .build()
                                )
                                .build()
                        )
                        .setQuantity(1L)
                        .build()
                )
                .putMetadata("orderId", String.valueOf(order.getId()))
                .putMetadata("customerId", String.valueOf(order.getCustomer().getId()))
                .build();

        Session session = Session.create(params);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotalPrice());
        payment.setMethod(PaymentMethod.STRIPE);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setTransactionId(session.getId());
        payment.setCreatedAt(LocalDateTime.now());

        paymentRepository.save(payment);

        log.info("Checkout session created successfully: {}", session.getId());
        
        return PaymentResponse.builder()
                .paymentId(session.getId())
                .sessionUrl(session.getUrl())
                .successUrl(successUrl)
                .cancelUrl(cancelUrl)
                .type("session")
                .build();
    }

    @Override
    @Transactional
    public void handlePaymentSuccess(String paymentId) throws StripeException {
        log.info("Handling payment success for session: {}", paymentId);

        Session session = Session.retrieve(paymentId);

        Payment payment = paymentRepository.findByTransactionId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for session: " + paymentId));

        if ("complete".equals(session.getStatus()) && "paid".equals(session.getPaymentStatus())) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);
            log.info("Payment session confirmed successfully: {}", paymentId);
        } else {
            log.warn("Payment session status is not complete/paid: {} / {}", session.getStatus(), session.getPaymentStatus());
        }
    }

    @Override
    @Transactional
    public void handlePaymentCancel(String paymentId) throws StripeException {
        log.info("Handling payment cancel for session: {}", paymentId);

        Session session = Session.retrieve(paymentId);

        Payment payment = paymentRepository.findByTransactionId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for session: " + paymentId));

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
        log.info("Payment session marked as failed: {}", paymentId);
    }

    @Override
    public String getServiceType() {
        return "session";
    }
}
