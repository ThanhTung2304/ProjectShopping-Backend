package com.example.fashionshop.entity;

import com.example.fashionshop.entity.OrderItem;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "order_code", nullable = false, unique = true, length = 20)
    private String orderCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "shipping_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "final_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "shipping_name", nullable = false, length = 100)
    private String shippingName;

    @Column(name = "shipping_phone", nullable = false, length = 20)
    private String shippingPhone;

    @Column(name = "shipping_address", nullable = false, length = 500)
    private String shippingAddress;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "ordered_at", nullable = false, updatable = false)
    private LocalDateTime orderedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private Payment payment;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private List<Review> reviews;

    @PrePersist
    public void prePersist() {
        orderedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum OrderStatus {
        PENDING, CONFIRMED, SHIPPING, DELIVERED, CANCELLED
    }
}
