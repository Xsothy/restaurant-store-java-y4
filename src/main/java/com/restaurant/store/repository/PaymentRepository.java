package com.restaurant.store.repository;

import com.restaurant.store.entity.Payment;
import com.restaurant.store.entity.PaymentMethod;
import com.restaurant.store.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    List<Payment> findByOrderId(Long orderId);
    
    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findByStatus(PaymentStatus status);

    Optional<Payment> findByOrderIdAndStatus(Long orderId, PaymentStatus status);

    Optional<Payment> findFirstByOrderIdAndMethodAndStatusInOrderByUpdatedAtDesc(
            Long orderId,
            PaymentMethod method,
            Collection<PaymentStatus> statuses
    );

    Optional<Payment> findFirstByOrderIdAndMethodAndStatusOrderByUpdatedAtDesc(
            Long orderId,
            PaymentMethod method,
            PaymentStatus status
    );
}
