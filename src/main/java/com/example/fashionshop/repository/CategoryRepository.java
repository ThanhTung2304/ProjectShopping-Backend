package com.example.fashionshop.repository;

import com.example.fashionshop.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    // Lấy danh mục gốc (không có parent)

    List<Category> findByParentIsNullAndIsActiveTrue();

    // Lấy danh mục con theo parent
    List<Category> findByParentIdAndIsActiveTrue(Long parentId);

}