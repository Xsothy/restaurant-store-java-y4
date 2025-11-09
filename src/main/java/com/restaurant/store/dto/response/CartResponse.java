package com.restaurant.store.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {
    private Long id;
    private List<CartItemResponse> items;
    private BigDecimal subtotal;
    private BigDecimal vat;
    private BigDecimal deliveryFee;
    private BigDecimal total;
    private Integer itemCount;
}
