package com.example.fashionshop.repository;

import com.example.fashionshop.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
