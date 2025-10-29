package com.restaurant.store.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class AuthResponse {
    
    private String token;
    private String tokenType = "Bearer";
    private Long expiresIn;
    private CustomerResponse customer;
    private LocalDateTime issuedAt;
    
    public AuthResponse(String token, Long expiresIn, CustomerResponse customer) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.customer = customer;
        this.issuedAt = LocalDateTime.now();
    }
}