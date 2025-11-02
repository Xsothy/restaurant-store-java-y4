package com.restaurant.store.mapper;

import com.restaurant.store.dto.response.CustomerResponse;
import com.restaurant.store.entity.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {
    
    public CustomerResponse toResponse(Customer customer) {
        if (customer == null) {
            return null;
        }
        
        return CustomerResponse.builder()
                .id(customer.getId())
                .name(customer.getName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .address(customer.getAddress())
                .createdAt(customer.getCreatedAt())
                .build();
    }
}
