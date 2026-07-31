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

    Optional<ProductVariant> findBySku(String sku);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    /**
     * Kiểm tra một sản phẩm đã có biến thể với cùng size và màu hay chưa.
     * IgnoreCase giúp "Đen" và "đen" được coi là cùng một màu.
     */
    boolean existsByProductIdAndSizeIgnoreCaseAndColorIgnoreCase(
            Long productId,
            String size,
            String color
    );

    /**
     * Dùng khi cập nhật biến thể.
     * Bỏ qua chính biến thể đang được sửa.
     */
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

    Optional<ProductVariant> findTopByProductIdOrderBySkuDesc(Long productId);

    List<ProductVariant> findByProductId(Long productId);

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

    @Query("""
        SELECT MAX(CAST(SUBSTRING(p.productCode,:length) AS integer))
        FROM Product p
        WHERE p.productCode LIKE CONCAT(:prefix,'%')
        """)

    Integer findMaxSkuSequenceByProductId(@Param("productId") Long productId);
}