package com.restaurant.store.service;

import com.restaurant.store.dto.response.DeliveryResponse;
import com.restaurant.store.entity.Customer;
import com.restaurant.store.entity.Delivery;
import com.restaurant.store.entity.Order;
import com.restaurant.store.exception.BadRequestException;
import com.restaurant.store.exception.ResourceNotFoundException;
import com.restaurant.store.repository.CustomerRepository;
import com.restaurant.store.repository.DeliveryRepository;
import com.restaurant.store.repository.OrderRepository;
import com.restaurant.store.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeliveryService {

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private JwtUtil jwtUtil;

    public DeliveryResponse getDeliveryByOrderId(Long orderId, String token) {
        String email = jwtUtil.extractUsername(token.substring(7));
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new BadRequestException("Order does not belong to current customer");
        }

        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found for order id: " + orderId));

        return mapToDeliveryResponse(delivery);
    }

    public DeliveryResponse trackDelivery(Long orderId, String token) {
        return getDeliveryByOrderId(orderId, token);
    }

    private DeliveryResponse mapToDeliveryResponse(Delivery delivery) {
        DeliveryResponse response = new DeliveryResponse();
        response.setId(delivery.getId());
        response.setOrderId(delivery.getOrder().getId());
        response.setDriverName(delivery.getDriverName());
        response.setDriverPhone(delivery.getDriverPhone());
        response.setVehicleInfo(delivery.getVehicleInfo());
        response.setStatus(delivery.getStatus().toString());
        response.setPickupTime(delivery.getPickupTime());
        response.setEstimatedArrivalTime(delivery.getEstimatedArrivalTime());
        response.setActualDeliveryTime(delivery.getActualDeliveryTime());
        response.setCurrentLocation(delivery.getCurrentLocation());
        response.setDeliveryNotes(delivery.getDeliveryNotes());
        return response;
    }
}
