package com.restaurant.store.integration.dto;

import lombok.Data;

@Data
public class AdminLoginResponse {
    private String token;
    private AdminUserDto user;
}
