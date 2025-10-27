package com.restaurant.store.repository;

import com.restaurant.store.entity.Delivery;
import com.restaurant.store.entity.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    
    Optional<Delivery> findByOrderId(Long orderId);
    
    List<Delivery> findByStatus(DeliveryStatus status);
    
    List<Delivery> findByDriverName(String driverName);
}