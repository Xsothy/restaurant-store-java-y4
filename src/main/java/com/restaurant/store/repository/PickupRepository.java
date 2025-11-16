package com.restaurant.store.repository;

import com.restaurant.store.entity.Pickup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PickupRepository extends JpaRepository<Pickup, Long> {
    Optional<Pickup> findByOrderId(Long orderId);
}
