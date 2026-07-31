package com.example.fashionshop.repository;

import com.example.fashionshop.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByProductCode(String productCode);

    Optional<Product> findByProductCode(String productCode);

    /**
     * Tìm productCode lớn nhất có cùng tiền tố.
     * Ví dụ database có:
     * AKC0001
     * AKC0002
     * AKC0003
     * Khi truyền prefix = AKC,
     * kết quả trả về là AKC0003.
     */
    Optional<Product> findTopByProductCodeStartingWithOrderByProductCodeDesc(
            String prefix
    );

    Page<Product> findByCategoryIdAndIsActiveTrue(
            Long categoryId,
            Pageable pageable
    );

    Page<Product> findByNameContainingIgnoreCaseAndIsActiveTrue(
            String name,
            Pageable pageable
    );

    List<Product> findByIsActiveTrueAndIsFeaturedTrue();

    @EntityGraph(attributePaths = {"category"})
    @Query("""
        SELECT p
        FROM Product p
        WHERE p.isActive = true
          AND (
                :keyword IS NULL
                OR :keyword = ''
                OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(p.productCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
          AND (
                :categoryId IS NULL
                OR p.category.id = :categoryId
          )
    """)
    Page<Product> search(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );
}