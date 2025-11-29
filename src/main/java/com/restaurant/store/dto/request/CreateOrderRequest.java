package com.restaurant.store.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.restaurant.store.entity.OrderType;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    
    @NotEmpty(message = "Order items cannot be empty")
    @Valid
    private List<OrderItemRequest> orderItems;
    
    @NotNull(message = "Order type is required")
    private OrderType orderType;
    
    private String deliveryAddress;
    
    @Size(max = 20, message = "Phone number must be less than 20 characters")
    private String phoneNumber;
    
    @Size(max = 500, message = "Special instructions must be less than 500 characters")
    private String specialInstructions;

    private Double latitude;

    private Double longitude;

    @AssertTrue(message = "Delivery address is required for delivery orders")
    private boolean isDeliveryAddressValid() {
        if (orderType != OrderType.DELIVERY) {
            return true;
        }
        return deliveryAddress != null && !deliveryAddress.isBlank();
    }

    @AssertTrue(message = "Delivery location is required for delivery orders")
    private boolean isDeliveryLocationValid() {
        if (orderType != OrderType.DELIVERY) {
            return true;
        }
        return latitude != null && longitude != null;
    }

    @AssertTrue(message = "Phone number is required for delivery orders")
    private boolean isDeliveryPhoneValid() {
        if (orderType != OrderType.DELIVERY) {
            return true;
        }
        return phoneNumber != null && !phoneNumber.isBlank();
    }

    @AssertTrue(message = "Phone number is required for pickup orders")
    private boolean isPickupPhoneValid() {
        if (orderType != OrderType.PICKUP) {
            return true;
        }
        return phoneNumber != null && !phoneNumber.isBlank();
    }
}
