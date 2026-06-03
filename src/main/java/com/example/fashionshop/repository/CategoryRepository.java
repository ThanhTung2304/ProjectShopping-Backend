package com.example.fashionshop.repository;

import com.example.fashionshop.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsBySlug(String slug);

    List<Category> findByParentIsNullAndIsActiveTrue();

    @Query("""
        SELECT DISTINCT c
        FROM Category c
        LEFT JOIN FETCH c.children
        WHERE c.parent IS NULL
          AND c.isActive = true
    """)
    List<Category> findRootCategoriesWithChildren();
}