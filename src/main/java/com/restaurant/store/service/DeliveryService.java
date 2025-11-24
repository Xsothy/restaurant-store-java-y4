package com.restaurant.store.service;

import com.restaurant.store.controller.api.DeliveryStatusWebSocketController;
import com.restaurant.store.dto.response.DeliveryResponse;
import com.restaurant.store.dto.response.OrderStatusMessage;
import com.restaurant.store.entity.Customer;
import com.restaurant.store.entity.Delivery;
import com.restaurant.store.entity.DeliveryStatus;
import com.restaurant.store.entity.Order;
import com.restaurant.store.exception.BadRequestException;
import com.restaurant.store.exception.ResourceNotFoundException;
import com.restaurant.store.mapper.DeliveryMapper;
import com.restaurant.store.repository.CustomerRepository;
import com.restaurant.store.repository.DeliveryRepository;
import com.restaurant.store.repository.OrderRepository;
import com.restaurant.store.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class DeliveryService {

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private DeliveryMapper deliveryMapper;

    @Autowired
    private DeliveryStatusWebSocketController deliveryStatusWebSocketController;

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

        return deliveryMapper.toResponse(delivery);
    }

    public DeliveryResponse trackDelivery(Long orderId, String token) {
        return getDeliveryByOrderId(orderId, token);
    }

    @Transactional
    public DeliveryResponse updateDeliveryStatus(Long orderId, DeliveryStatus newStatus, String location) {
        log.info("Updating delivery status for order: {} - New status: {}", orderId, newStatus);

        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found for order id: " + orderId));

        DeliveryStatus oldStatus = delivery.getStatus();
        delivery.setStatus(newStatus);

        if (location != null) {
            delivery.setCurrentLocation(location);
        }

        if (newStatus == DeliveryStatus.PICKED_UP && delivery.getPickupTime() == null) {
            delivery.setPickupTime(LocalDateTime.now());
        }

        if (newStatus == DeliveryStatus.DELIVERED && delivery.getActualDeliveryTime() == null) {
            delivery.setActualDeliveryTime(LocalDateTime.now());
        }

        delivery = deliveryRepository.save(delivery);

        DeliveryResponse response = deliveryMapper.toResponse(delivery);
        deliveryStatusWebSocketController.sendDeliveryUpdate(orderId, response);

        OrderStatusMessage statusMessage = buildStatusMessage(orderId, oldStatus, newStatus, location);
        deliveryStatusWebSocketController.sendDeliveryStatusUpdate(orderId, statusMessage);

        if (shouldSendNotification(oldStatus, newStatus)) {
            OrderStatusMessage notification = buildNotificationMessage(orderId, newStatus, delivery);
            deliveryStatusWebSocketController.sendDeliveryNotification(orderId, notification);
        }

        log.info("Delivery status updated successfully for order: {} - {} -> {}", orderId, oldStatus, newStatus);
        return response;
    }

    @Transactional
    public void updateDeliveryLocation(Long orderId, String location) {
        log.info("Updating delivery location for order: {} - Location: {}", orderId, location);

        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found for order id: " + orderId));

        delivery.setCurrentLocation(location);
        deliveryRepository.save(delivery);

        deliveryStatusWebSocketController.sendLocationUpdate(orderId, location);
        log.info("Delivery location updated for order: {}", orderId);
    }

    @Transactional
    public void updateDeliveryLocation(Long orderId, Double latitude, Double longitude) {
        log.info("Updating delivery location for order: {} - Lat: {}, Lng: {}", orderId, latitude, longitude);

        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found for order id: " + orderId));

        delivery.setLatitude(latitude);
        delivery.setLongitude(longitude);
        
        if (latitude != null && longitude != null) {
            delivery.setCurrentLocation(String.format("%.6f,%.6f", latitude, longitude));
        }
        
        deliveryRepository.save(delivery);

        deliveryStatusWebSocketController.sendLocationUpdate(orderId, 
                String.format("{\"latitude\":%.6f,\"longitude\":%.6f}", latitude, longitude));
        log.info("Delivery location updated for order: {}", orderId);
    }

    @Transactional
    public DeliveryResponse assignDriver(Long orderId, String driverName, String driverPhone, String vehicleInfo) {
        log.info("Assigning driver to delivery for order: {} - Driver: {}", orderId, driverName);

        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found for order id: " + orderId));

        delivery.setDriverName(driverName);
        delivery.setDriverPhone(driverPhone);
        delivery.setVehicleInfo(vehicleInfo);
        delivery.setStatus(DeliveryStatus.ASSIGNED);

        delivery = deliveryRepository.save(delivery);

        DeliveryResponse response = deliveryMapper.toResponse(delivery);
        deliveryStatusWebSocketController.sendDeliveryUpdate(orderId, response);

        OrderStatusMessage notification = OrderStatusMessage.builder()
                .orderId(orderId)
                .eventType("DRIVER_ASSIGNED")
                .status(DeliveryStatus.ASSIGNED.name())
                .title("Driver Assigned")
                .message("Your driver " + driverName + " has been assigned to your delivery")
                .timestamp(LocalDateTime.now())
                .build();
        deliveryStatusWebSocketController.sendDeliveryNotification(orderId, notification);

        log.info("Driver assigned successfully to order: {}", orderId);
        return response;
    }

    private OrderStatusMessage buildStatusMessage(Long orderId, DeliveryStatus oldStatus, 
                                                    DeliveryStatus newStatus, String location) {
        return OrderStatusMessage.builder()
                .orderId(orderId)
                .eventType("DELIVERY_STATUS_CHANGED")
                .status(newStatus.name())
                .title(getStatusTitle(newStatus))
                .message(getStatusMessage(newStatus))
                .timestamp(LocalDateTime.now())
                .build();
    }

    private OrderStatusMessage buildNotificationMessage(Long orderId, DeliveryStatus status, Delivery delivery) {
        return OrderStatusMessage.builder()
                .orderId(orderId)
                .eventType(getNotificationEvent(status))
                .status(status.name())
                .title(getNotificationTitle(status))
                .message(getNotificationMessage(status, delivery))
                .timestamp(LocalDateTime.now())
                .build();
    }

    private boolean shouldSendNotification(DeliveryStatus oldStatus, DeliveryStatus newStatus) {
        return newStatus == DeliveryStatus.ASSIGNED ||
               newStatus == DeliveryStatus.PICKED_UP ||
               newStatus == DeliveryStatus.ON_THE_WAY ||
               newStatus == DeliveryStatus.DELIVERED;
    }

    private String getStatusTitle(DeliveryStatus status) {
        return switch (status) {
            case PENDING -> "Delivery Pending";
            case ASSIGNED -> "Driver Assigned";
            case PICKED_UP -> "Order Picked Up";
            case ON_THE_WAY -> "On the Way";
            case DELIVERED -> "Delivered";
            case CANCELLED -> "Delivery Cancelled";
        };
    }

    private String getStatusMessage(DeliveryStatus status) {
        return switch (status) {
            case PENDING -> "Your delivery is pending assignment";
            case ASSIGNED -> "A driver has been assigned to your delivery";
            case PICKED_UP -> "Your order has been picked up by the driver";
            case ON_THE_WAY -> "Your order is on the way";
            case DELIVERED -> "Your order has been delivered";
            case CANCELLED -> "Your delivery has been cancelled";
        };
    }

    private String getNotificationEvent(DeliveryStatus status) {
        return switch (status) {
            case ASSIGNED -> "DRIVER_ASSIGNED";
            case PICKED_UP -> "ORDER_PICKED_UP";
            case ON_THE_WAY -> "DRIVER_ON_THE_WAY";
            case DELIVERED -> "ORDER_DELIVERED";
            default -> "DELIVERY_UPDATE";
        };
    }

    private String getNotificationTitle(DeliveryStatus status) {
        return switch (status) {
            case ASSIGNED -> "Driver Assigned";
            case PICKED_UP -> "Order Picked Up";
            case ON_THE_WAY -> "Driver On The Way";
            case DELIVERED -> "Order Delivered";
            default -> "Delivery Update";
        };
    }

    private String getNotificationMessage(DeliveryStatus status, Delivery delivery) {
        return switch (status) {
            case ASSIGNED -> delivery.getDriverName() != null
                    ? "Your driver " + delivery.getDriverName() + " will deliver your order"
                    : "A driver has been assigned to your delivery";
            case PICKED_UP -> "Your order is with the driver and on its way to you";
            case ON_THE_WAY -> delivery.getEstimatedArrivalTime() != null
                    ? "Your order will arrive around " + delivery.getEstimatedArrivalTime()
                    : "Your driver is on the way";
            case DELIVERED -> "Your order has been successfully delivered. Enjoy your meal!";
            default -> "Your delivery status has been updated";
        };
    }

    /**
     * Updates delivery with data from admin backend
     * @param orderId Order ID
     * @param deliveryStatus Delivery status
     * @param latitude Driver latitude
     * @param longitude Driver longitude
     * @param driverName Driver name
     * @param driverPhone Driver phone
     * @param deliveryAddress Delivery address
     * @param deliveryNotes Delivery notes
     * @param dispatchedAt Dispatched time
     * @param deliveredAt Delivered time
     * @return true if delivery was updated
     */
    @Transactional
    public boolean updateDeliveryFromAdmin(Long orderId, DeliveryStatus deliveryStatus,
                                          Double latitude, Double longitude,
                                          String driverName, String driverPhone,
                                          String deliveryAddress, String deliveryNotes,
                                          LocalDateTime dispatchedAt, LocalDateTime deliveredAt) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId).orElse(null);
        if (delivery == null) {
            log.debug("No delivery found for order {}, skipping delivery update", orderId);
            return false;
        }

        boolean updated = false;

        // Update delivery status
        if (deliveryStatus != null && deliveryStatus != delivery.getStatus()) {
            delivery.setStatus(deliveryStatus);
            updated = true;
            log.debug("Updated delivery status for order {} to {}", orderId, deliveryStatus);
        }

        // Update latitude
        if (latitude != null && !latitude.equals(delivery.getLatitude())) {
            delivery.setLatitude(latitude);
            updated = true;
        }

        // Update longitude
        if (longitude != null && !longitude.equals(delivery.getLongitude())) {
            delivery.setLongitude(longitude);
            updated = true;
        }

        // Update current location if coordinates are available
        if (latitude != null && longitude != null) {
            String locationString = String.format("%.6f,%.6f", latitude, longitude);
            if (!locationString.equals(delivery.getCurrentLocation())) {
                delivery.setCurrentLocation(locationString);
                updated = true;
                log.debug("Updated delivery location for order {} to {}", orderId, locationString);
            }
        }

        // Update delivery address if provided
        if (deliveryAddress != null && !deliveryAddress.equals(delivery.getDeliveryAddress())) {
            delivery.setDeliveryAddress(deliveryAddress);
            updated = true;
        }

        // Update delivery notes if provided
        if (deliveryNotes != null && !deliveryNotes.equals(delivery.getDeliveryNotes())) {
            delivery.setDeliveryNotes(deliveryNotes);
            updated = true;
        }

        // Update driver information if available
        if (driverName != null && !driverName.equals(delivery.getDriverName())) {
            delivery.setDriverName(driverName);
            updated = true;
        }

        if (driverPhone != null && !driverPhone.equals(delivery.getDriverPhone())) {
            delivery.setDriverPhone(driverPhone);
            updated = true;
        }

        // Update timestamps
        if (dispatchedAt != null && !dispatchedAt.equals(delivery.getEstimatedDeliveryTime())) {
            delivery.setEstimatedDeliveryTime(dispatchedAt);
            updated = true;
        }

        if (deliveredAt != null && !deliveredAt.equals(delivery.getActualDeliveryTime())) {
            delivery.setActualDeliveryTime(deliveredAt);
            updated = true;
        }

        if (updated) {
            deliveryRepository.save(delivery);
            log.info("Updated delivery for order {} with admin data (status: {}, lat: {}, lng: {})",
                    orderId, deliveryStatus, latitude, longitude);
        }

        return updated;
    }
}

