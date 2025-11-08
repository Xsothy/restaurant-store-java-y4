package com.restaurant.store.integration.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminCategoryDto {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
