package com.restaurant.store.dto.response;

import java.time.LocalDateTime;

public class AuthResponse {
    
    private String token;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private CustomerResponse customer;
    private LocalDateTime issuedAt;
    
    // Constructors
    public AuthResponse() {
        this.issuedAt = LocalDateTime.now();
    }
    
    public AuthResponse(String token, Long expiresIn, CustomerResponse customer) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.customer = customer;
        this.issuedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getTokenType() {
        return tokenType;
    }
    
    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
    
    public Long getExpiresIn() {
        return expiresIn;
    }
    
    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }
    
    public CustomerResponse getCustomer() {
        return customer;
    }
    
    public void setCustomer(CustomerResponse customer) {
        this.customer = customer;
    }
    
    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }
    
    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }
}