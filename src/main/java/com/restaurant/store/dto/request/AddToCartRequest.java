package com.restaurant.store.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to add a product to the shopping cart")
public class AddToCartRequest {
    
    @Schema(description = "ID of the product to add", example = "1", required = true)
    @NotNull(message = "Product ID is required")
    private Long productId;
    
    @Schema(description = "Quantity of the product to add", example = "2", required = true, minimum = "1")
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
