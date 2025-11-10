package com.restaurant.store.controller.api;

import com.restaurant.store.dto.response.ApiResponse;
import com.restaurant.store.entity.Order;
import com.restaurant.store.entity.OrderStatus;
import com.restaurant.store.exception.StripeWebhookException;
import com.restaurant.store.repository.OrderRepository;
import com.restaurant.store.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    private final PaymentService paymentService;
    private final OrderRepository orderRepository;
    
    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/stripe")
    public ResponseEntity<ApiResponse<String>> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        log.info("Received Stripe webhook");

        Event event = constructEvent(payload, sigHeader);
        log.info("Processing webhook event type: {}", event.getType());

        StripeObject stripeObject = getStripeObject(event);
        dispatchEvent(event, stripeObject);

        return ResponseEntity.ok(ApiResponse.success("Webhook processed successfully", event.getType()));
    }

    private Event constructEvent(String payload, String signature) {
        try {
            return Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new StripeWebhookException("Invalid Stripe webhook signature", HttpStatus.BAD_REQUEST, e);
        }
    }

    private StripeObject getStripeObject(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        return deserializer.getObject()
                .orElseThrow(() -> new StripeWebhookException(
                        "Unable to deserialize event data for type " + event.getType(),
                        HttpStatus.BAD_REQUEST
                ));
    }

    private void dispatchEvent(Event event, StripeObject stripeObject) {
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
    }

    private void handlePaymentIntentSucceeded(PaymentIntent paymentIntent) {
        log.info("Payment intent succeeded: {}", paymentIntent.getId());

        try {
            paymentService.handlePaymentSuccess(paymentIntent.getId());
        } catch (StripeException e) {
            throw new StripeWebhookException("Unable to confirm payment intent", e);
        }

        updateOrderStatusFromMetadata(paymentIntent.getMetadata().get("orderId"), OrderStatus.CONFIRMED);
    }

    private void handlePaymentIntentFailed(PaymentIntent paymentIntent) {
        log.info("Payment intent failed: {}", paymentIntent.getId());

        try {
            paymentService.handlePaymentCancel(paymentIntent.getId());
        } catch (StripeException e) {
            throw new StripeWebhookException("Unable to cancel payment intent", e);
        }
    }

    private void handleCheckoutSessionCompleted(Session session) {
        log.info("Checkout session completed: {}", session.getId());

        try {
            paymentService.handlePaymentSuccess(session.getId());
        } catch (StripeException e) {
            throw new StripeWebhookException("Unable to confirm checkout session", e);
        }

        updateOrderStatusFromMetadata(session.getMetadata().get("orderId"), OrderStatus.CONFIRMED);
    }

    private void handleCheckoutSessionExpired(Session session) {
        log.info("Checkout session expired: {}", session.getId());

        try {
            paymentService.handlePaymentCancel(session.getId());
        } catch (StripeException e) {
            throw new StripeWebhookException("Unable to cancel checkout session", e);
        }
    }

    private void updateOrderStatusFromMetadata(String orderIdMetadata, OrderStatus status) {
        if (orderIdMetadata == null) {
            log.debug("No orderId metadata found on Stripe payload");
            return;
        }

        try {
            Long orderId = Long.parseLong(orderIdMetadata);
            updateOrderStatus(orderId, status);
        } catch (NumberFormatException ex) {
            throw new StripeWebhookException(
                    "Invalid orderId metadata value: " + orderIdMetadata,
                    HttpStatus.BAD_REQUEST,
                    ex
            );
        }
    }

    private void updateOrderStatus(Long orderId, OrderStatus status) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        orderOpt.ifPresentOrElse(order -> {
            order.setStatus(status);
            orderRepository.save(order);
            log.info("Order {} status updated to {}", orderId, status);
        }, () -> log.warn("Order {} referenced in Stripe metadata was not found", orderId));
    }
}
