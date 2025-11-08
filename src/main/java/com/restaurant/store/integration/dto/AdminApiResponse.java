package com.restaurant.store.integration.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminApiResponse<T> {
    private Boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;
}
