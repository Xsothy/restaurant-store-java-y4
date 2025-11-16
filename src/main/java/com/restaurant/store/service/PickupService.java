package com.restaurant.store.service;

import com.restaurant.store.dto.response.PickupResponse;
import com.restaurant.store.entity.Customer;
import com.restaurant.store.entity.Order;
import com.restaurant.store.entity.OrderType;
import com.restaurant.store.entity.Pickup;
import com.restaurant.store.exception.BadRequestException;
import com.restaurant.store.exception.ResourceNotFoundException;
import com.restaurant.store.mapper.PickupMapper;
import com.restaurant.store.repository.CustomerRepository;
import com.restaurant.store.repository.OrderRepository;
import com.restaurant.store.repository.PickupRepository;
import com.restaurant.store.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PickupService {

    private final PickupRepository pickupRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final JwtUtil jwtUtil;
    private final PickupMapper pickupMapper;

    public PickupResponse getPickupDetails(Long orderId, String token) {
        Customer customer = resolveCustomer(token);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new BadRequestException("Order does not belong to current customer");
        }

        if (order.getOrderType() != OrderType.PICKUP) {
            throw new BadRequestException("Order is not a pickup order");
        }

        Pickup pickup = pickupRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pickup details not found for order id: " + orderId));

        return pickupMapper.toResponse(pickup);
    }

    private Customer resolveCustomer(String token) {
        String email = jwtUtil.extractUsername(token.substring(7));
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }
}
