package com.example.fashionshop.repository;

import com.example.fashionshop.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository
        extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductIdAndIsActiveTrue(Long productId);

    List<ProductVariant> findByProductId(Long productId);

    Optional<ProductVariant> findBySku(String sku);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    boolean existsByProductIdAndSizeIgnoreCaseAndColorIgnoreCase(
            Long productId,
            String size,
            String color
    );

    boolean existsByProductIdAndSizeIgnoreCaseAndColorIgnoreCaseAndIdNot(
            Long productId,
            String size,
            String color,
            Long id
    );

    Optional<ProductVariant> findByProductIdAndSizeAndColor(
            Long productId,
            String size,
            String color
    );

    /**
     * Lấy biến thể có SKU lớn nhất của một sản phẩm.
     *
     * Ví dụ:
     * AKC0001-001
     * AKC0001-002
     * AKC0001-003
     *
     * Kết quả sẽ trả về AKC0001-003.
     */
    Optional<ProductVariant> findTopByProductIdOrderBySkuDesc(Long productId);

    // Trừ tồn kho có điều kiện — atomic, chống oversell
    @Modifying
    @Query("""
            UPDATE ProductVariant pv
            SET pv.stockQuantity = pv.stockQuantity - :quantity
            WHERE pv.id = :variantId
              AND pv.stockQuantity >= :quantity
            """)
    int decreaseStock(
            @Param("variantId") Long variantId,
            @Param("quantity") int quantity
    );

    // Hoàn lại tồn kho khi hủy đơn
    @Modifying
    @Query("""
            UPDATE ProductVariant pv
            SET pv.stockQuantity = pv.stockQuantity + :quantity
            WHERE pv.id = :variantId
            """)
    void increaseStock(
            @Param("variantId") Long variantId,
            @Param("quantity") int quantity
    );

    @Query("""
            SELECT COUNT(v)
            FROM ProductVariant v
            WHERE v.product.id = :productId
              AND v.isActive = true
            """)
    long countByProductId(
            @Param("productId") Long productId
    );
}