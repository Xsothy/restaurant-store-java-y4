package com.restaurant.store.security;

import com.restaurant.store.entity.Customer;
import com.restaurant.store.exception.UnauthorizedException;
import com.restaurant.store.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Authentication helper utility similar to Laravel's auth() helper.
 * Provides convenient methods to access the currently authenticated customer.
 * 
 * Usage:
 * - authHelper.user() - Get current authenticated customer or throw exception
 * - authHelper.userOrNull() - Get current authenticated customer or null
 * - authHelper.id() - Get current user ID or throw exception
 * - authHelper.check() - Check if user is authenticated
 */
@Component
@RequiredArgsConstructor
public class AuthHelper {

    private final CustomerRepository customerRepository;

    /**
     * Get the currently authenticated customer.
     * 
     * @return the authenticated Customer
     * @throws UnauthorizedException if no user is authenticated
     */
    public Customer user() {
        return userOrNull()
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));
    }

    /**
     * Get the currently authenticated customer as Optional.
     * 
     * @return Optional containing the Customer if authenticated, empty otherwise
     */
    public Optional<Customer> userOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || 
            authentication.getPrincipal().equals("anonymousUser")) {
            return Optional.empty();
        }

        String email = authentication.getName();
        return customerRepository.findByEmail(email);
    }

    /**
     * Get the ID of the currently authenticated customer.
     * 
     * @return the customer ID
     * @throws UnauthorizedException if no user is authenticated
     */
    public Long id() {
        return user().getId();
    }

    /**
     * Check if a user is currently authenticated.
     * 
     * @return true if a user is authenticated, false otherwise
     */
    public boolean check() {
        return userOrNull().isPresent();
    }

    /**
     * Check if no user is currently authenticated (guest).
     * 
     * @return true if no user is authenticated, false otherwise
     */
    public boolean guest() {
        return !check();
    }

    /**
     * Get the email of the currently authenticated customer.
     * 
     * @return the customer email
     * @throws UnauthorizedException if no user is authenticated
     */
    public String email() {
        return user().getEmail();
    }
}
