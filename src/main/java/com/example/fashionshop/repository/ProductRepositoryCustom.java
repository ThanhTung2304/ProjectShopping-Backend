package com.example.fashionshop.repository;

import com.example.fashionshop.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface ProductRepositoryCustom {
    /**
     * Tìm kiếm sản phẩm với nhiều điều kiện filter:
     * - keyword: tìm theo tên sản phẩm
     * - categoryId: lọc theo danh mục
     * - minPrice / maxPrice: lọc theo khoảng giá (từ product_variants)
     * - size / color: lọc theo size hoặc màu sắc có sẵn
     */

    Page<Product> searchProducts(
            String keyword,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String size,
            String color,
            Pageable pageable
    );
}
