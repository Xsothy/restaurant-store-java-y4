package com.restaurant.store.controller;

import com.restaurant.store.dto.request.CustomerRegisterRequest;
import com.restaurant.store.dto.request.LoginRequest;
import com.restaurant.store.dto.response.ApiResponse;
import com.restaurant.store.dto.response.AuthResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    // TODO: Inject AuthService when implemented
    // private final AuthService authService;
    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody CustomerRegisterRequest request) {
        
        // TODO: Implement customer registration logic
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer registered successfully", null));
    }
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        
        // TODO: Implement login logic
        return ResponseEntity.ok(ApiResponse.success("Login successful", null));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @RequestHeader("Authorization") String authToken) {
        
        // TODO: Implement logout logic (if needed, mostly handled client-side)
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }
}