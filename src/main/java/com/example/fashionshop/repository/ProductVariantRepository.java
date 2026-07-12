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
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductIdAndIsActiveTrue(Long productId);

    Optional<ProductVariant> findBySku(String sku);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    //Tìm variant theo product + size + color
    Optional<ProductVariant> findByProductIdAndSizeAndColor(Long productId, String size, String color);

    List<ProductVariant> findByProductId(Long productId);

    // ========================
    // Trừ tồn kho có điều kiện — atomic, chống oversell
    // ========================
    @Modifying
    @Query("UPDATE ProductVariant pv SET pv.stockQuantity = pv.stockQuantity - :quantity WHERE pv.id = :variantId AND pv.stockQuantity >= :quantity")
    int decreaseStock(@Param("variantId") Long variantId, @Param("quantity") int quantity);

    // ========================
    // Hoàn lại tồn kho (khi hủy đơn) — atomic
    // ========================
    @Modifying
    @Query("UPDATE ProductVariant pv SET pv.stockQuantity = pv.stockQuantity + :quantity WHERE pv.id = :variantId")
    void increaseStock(@Param("variantId") Long variantId, @Param("quantity") int quantity);
}
