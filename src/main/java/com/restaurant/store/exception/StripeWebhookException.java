package com.restaurant.store.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a Stripe webhook event cannot be processed successfully.
 */
public class StripeWebhookException extends RuntimeException {

    private final HttpStatus status;

    public StripeWebhookException(String message) {
        this(message, HttpStatus.INTERNAL_SERVER_ERROR, null);
    }

    public StripeWebhookException(String message, Throwable cause) {
        this(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }

    public StripeWebhookException(String message, HttpStatus status) {
        this(message, status, null);
    }

    public StripeWebhookException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
