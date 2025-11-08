package com.restaurant.store.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class JwtExpiredException extends JwtAuthenticationException {
    public JwtExpiredException(String message) {
        super(message);
    }
    
    public JwtExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
