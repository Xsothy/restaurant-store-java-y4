package com.restaurant.store.integration.dto;

import lombok.Data;

@Data
public class AdminLoginResponse {
    private Boolean success;
    private String message;
    private String token;
    private AdminUserDto user;
}
