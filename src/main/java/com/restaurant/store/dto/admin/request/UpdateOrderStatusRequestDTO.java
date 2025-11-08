package com.restaurant.store.dto.admin.request;

import com.restaurant.store.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequestDTO {
    @NotNull(message = "Status is required")
    private OrderStatus status;
}