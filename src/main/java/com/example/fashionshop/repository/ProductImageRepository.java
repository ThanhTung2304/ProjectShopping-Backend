package com.example.fashionshop.repository;

import com.example.fashionshop.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductIdOrderBySortOrderAsc(Long productId);

    Optional<ProductImage> findByProductIdAndIsPrimaryTrue(Long productId);

    Optional<ProductImage> findByIdAndProductId(Long id, Long productId);

    // Bỏ primary tất cả ảnh của product trước khi set primary mới
    @Modifying
    @Query("UPDATE ProductImage pi SET pi.isPrimary = false WHERE pi.product.id = :productId")
    void clearPrimaryByProductId(Long productId);
}
