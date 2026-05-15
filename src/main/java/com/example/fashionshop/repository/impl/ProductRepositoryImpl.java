package com.example.fashionshop.repository.impl;

import com.example.fashionshop.entity.Product;
import com.example.fashionshop.repository.ProductRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<Product> searchProducts(
            String keyword,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String size,
            String color,
            Pageable pageable
    ) {
        // Build điều kiện WHERE động
        StringBuilder where = new StringBuilder("WHERE p.isActive = true ");
        Map<String, Object> params = new HashMap<>();

        if (keyword != null && !keyword.isBlank()) {
            where.append("AND LOWER(p.name) LIKE :keyword ");
            params.put("keyword", "%" + keyword.toLowerCase() + "%");
        }

        if (categoryId != null) {
            where.append("AND p.category.id = :categoryId ");
            params.put("categoryId", categoryId);
        }

        // Filter theo giá, size, color → cần JOIN với product_variants
        boolean needVariantJoin = minPrice != null || maxPrice != null
                || (size != null && !size.isBlank())
                || (color != null && !color.isBlank());

        String join = needVariantJoin
                ? "JOIN p.variants v "
                : "";

        if (needVariantJoin) {
            where.append("AND v.isActive = true ");
        }

        if (minPrice != null) {
            where.append("AND (v.salePrice IS NOT NULL AND v.salePrice >= :minPrice " +
                    "OR v.salePrice IS NULL AND v.price >= :minPrice) ");
            params.put("minPrice", minPrice);
        }

        if (maxPrice != null) {
            where.append("AND (v.salePrice IS NOT NULL AND v.salePrice <= :maxPrice " +
                    "OR v.salePrice IS NULL AND v.price <= :maxPrice) ");
            params.put("maxPrice", maxPrice);
        }

        if (size != null && !size.isBlank()) {
            where.append("AND LOWER(v.size) = :size ");
            params.put("size", size.toLowerCase());
        }

        if (color != null && !color.isBlank()) {
            where.append("AND LOWER(v.color) = :color ");
            params.put("color", color.toLowerCase());
        }

        // Query lấy dữ liệu (DISTINCT tránh trùng khi JOIN variants)
        String jpql = "SELECT DISTINCT p FROM Product p " + join + where;
        TypedQuery<Product> query = em.createQuery(jpql, Product.class);

        // Query đếm tổng (cho phân trang)
        String countJpql = "SELECT COUNT(DISTINCT p) FROM Product p " + join + where;
        TypedQuery<Long> countQuery = em.createQuery(countJpql, Long.class);

        // Set params cho cả 2 query
        params.forEach((k, v) -> {
            query.setParameter(k, v);
            countQuery.setParameter(k, v);
        });

        // Phân trang
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<Product> results = query.getResultList();
        Long total = countQuery.getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }
}