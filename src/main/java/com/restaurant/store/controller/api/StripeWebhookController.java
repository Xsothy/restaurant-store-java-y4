package com.restaurant.store.controller.api;

import com.restaurant.store.entity.Order;
import com.restaurant.store.entity.OrderStatus;
import com.restaurant.store.repository.OrderRepository;
import com.restaurant.store.repository.PaymentRepository;
import com.restaurant.store.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    
    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        
        log.info("Received Stripe webhook");
        
        Event event;
        
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Invalid webhook signature", e);
            return ResponseEntity.badRequest().body("Invalid signature");
        }
        
        log.info("Processing webhook event type: {}", event.getType());
        
        try {
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject = null;
            
            if (dataObjectDeserializer.getObject().isPresent()) {
                stripeObject = dataObjectDeserializer.getObject().get();
            } else {
                log.warn("Unable to deserialize event data object");
                return ResponseEntity.ok("Event received but not processed");
            }
            
            switch (event.getType()) {
                case "payment_intent.succeeded":
                    handlePaymentIntentSucceeded((PaymentIntent) stripeObject);
                    break;
                    
                case "payment_intent.payment_failed":
                    handlePaymentIntentFailed((PaymentIntent) stripeObject);
                    break;
                    
                case "checkout.session.completed":
                    handleCheckoutSessionCompleted((Session) stripeObject);
                    break;
                    
                case "checkout.session.expired":
                    handleCheckoutSessionExpired((Session) stripeObject);
                    break;
                    
                default:
                    log.info("Unhandled event type: {}", event.getType());
            }
            
            return ResponseEntity.ok("Webhook processed successfully");
        } catch (Exception e) {
            log.error("Error processing Stripe webhook", e);
            return ResponseEntity.status(500).body("Webhook processing failed: " + e.getMessage());
        }
    }
    
    private void handlePaymentIntentSucceeded(PaymentIntent paymentIntent) {
        try {
            log.info("Payment intent succeeded: {}", paymentIntent.getId());
            paymentService.handlePaymentSuccess(paymentIntent.getId());
            
            String orderId = paymentIntent.getMetadata().get("orderId");
            if (orderId != null) {
                updateOrderStatus(Long.parseLong(orderId), OrderStatus.CONFIRMED);
            }
        } catch (Exception e) {
            log.error("Error handling payment intent succeeded", e);
        }
    }
    
    private void handlePaymentIntentFailed(PaymentIntent paymentIntent) {
        try {
            log.info("Payment intent failed: {}", paymentIntent.getId());
            paymentService.handlePaymentCancel(paymentIntent.getId());
        } catch (Exception e) {
            log.error("Error handling payment intent failed", e);
        }
    }
    
    private void handleCheckoutSessionCompleted(Session session) {
        try {
            log.info("Checkout session completed: {}", session.getId());
            paymentService.handlePaymentSuccess(session.getId());
            
            String orderId = session.getMetadata().get("orderId");
            if (orderId != null) {
                updateOrderStatus(Long.parseLong(orderId), OrderStatus.CONFIRMED);
            }
        } catch (Exception e) {
            log.error("Error handling checkout session completed", e);
        }
    }
    
    private void handleCheckoutSessionExpired(Session session) {
        try {
            log.info("Checkout session expired: {}", session.getId());
            paymentService.handlePaymentCancel(session.getId());
        } catch (Exception e) {
            log.error("Error handling checkout session expired", e);
        }
    }
    
    private void updateOrderStatus(Long orderId, OrderStatus status) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setStatus(status);
            orderRepository.save(order);
            log.info("Order {} status updated to {}", orderId, status);
        }
    }
}
