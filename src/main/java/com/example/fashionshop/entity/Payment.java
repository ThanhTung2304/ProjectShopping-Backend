package com.example.fashionshop.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // ===== Các field bổ sung phục vụ đối soát / audit với VNPay =====

    @Column(name = "response_code", length = 5)
    private String responseCode;

    @Column(name = "transaction_status", length = 5)
    private String transactionStatus;

    @Column(name = "bank_code", length = 20)
    private String bankCode;

    /** Thời điểm VNPay xác nhận thanh toán (vnp_PayDate), giờ Việt Nam. */
    @Column(name = "pay_date")
    private LocalDateTime payDate;

    public enum PaymentStatus {
        PENDING, PAID, FAILED, REFUNDED
    }

    // Payment.java
    public enum PaymentMethod {
        VNPAY, MOMO, BANK_TRANSFER, COD;

        @JsonCreator
        public static PaymentMethod fromString(String value) {
            return PaymentMethod.valueOf(value.toUpperCase());
        }
    }
}