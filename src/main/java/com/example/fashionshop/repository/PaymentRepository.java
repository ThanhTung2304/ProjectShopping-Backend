package com.example.fashionshop.repository;

import com.example.fashionshop.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByOrderOrderCode(String orderCode);

    List<Payment> findByStatusAndOrderOrderedAtBefore(
            Payment.PaymentStatus status,
            LocalDateTime threshold
    );

    List<Payment> findByMethodAndStatusAndOrderOrderedAtBefore(
            Payment.PaymentMethod method,
            Payment.PaymentStatus status,
            LocalDateTime threshold
    );
}