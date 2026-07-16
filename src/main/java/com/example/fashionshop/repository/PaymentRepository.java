package com.example.fashionshop.repository;

import com.example.fashionshop.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    /**
     * Khóa Payment theo orderId.
     * Dùng khi khách/admin hủy đơn để tránh chạy đồng thời với IPN hoặc scheduler.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM Payment p
            JOIN FETCH p.order o
            WHERE o.id = :orderId
            """)
    Optional<Payment> findByOrderIdWithLock(@Param("orderId") Long orderId);

    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByOrderOrderCode(String orderCode);

    /**
     * Khóa Payment và lấy luôn Order để:
     * - tránh race condition;
     * - tránh LazyInitializationException.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM Payment p
            JOIN FETCH p.order o
            WHERE o.orderCode = :orderCode
            """)
    Optional<Payment> findByOrderCodeWithLock(
            @Param("orderCode") String orderCode
    );

    /**
     * Chỉ lấy và khóa các Payment VNPay đang PENDING đã hết hạn.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p
            FROM Payment p
            JOIN FETCH p.order o
            WHERE p.method = :method
              AND p.status = :status
              AND o.orderedAt < :threshold
            """)
    List<Payment> findExpiredPaymentsWithLock(
            @Param("method") Payment.PaymentMethod method,
            @Param("status") Payment.PaymentStatus status,
            @Param("threshold") LocalDateTime threshold
    );
}