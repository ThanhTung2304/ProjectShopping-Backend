package com.example.fashionshop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
        name = "products",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_products_product_code",
                        columnNames = "product_code"
                ),
                @UniqueConstraint(
                        name = "uk_products_slug",
                        columnNames = "slug"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Mã sản phẩm được backend tự động tạo từ tên sản phẩm.
     */
    @Column(
            name = "product_code",
            nullable = false,
            unique = true,
            length = 30
    )
    private String productCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(
            name = "size_type",
            nullable = false,
            length = 20,
            columnDefinition = "VARCHAR(20) DEFAULT 'CLOTHING'"
    )
    private SizeType sizeType = SizeType.CLOTHING;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "is_featured", nullable = false)
    private Boolean isFeatured = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY
    )
    private List<ProductVariant> variants;

    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY
    )
    private List<ProductImage> images;

    @OneToMany(
            mappedBy = "product",
            fetch = FetchType.LAZY
    )
    private List<Review> reviews;

    @PrePersist
    public void prePersist() {
        if (sizeType == null) {
            sizeType = SizeType.CLOTHING;
        }

        if (isActive == null) {
            isActive = true;
        }

        if (isFeatured == null) {
            isFeatured = false;
        }

        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        if (sizeType == null) {
            sizeType = SizeType.CLOTHING;
        }

        if (isActive == null) {
            isActive = true;
        }

        if (isFeatured == null) {
            isFeatured = false;
        }

        updatedAt = LocalDateTime.now();
    }

    public enum SizeType {
        CLOTHING,
        PANTS,
        SHOES,
        FREE_SIZE
    }
}