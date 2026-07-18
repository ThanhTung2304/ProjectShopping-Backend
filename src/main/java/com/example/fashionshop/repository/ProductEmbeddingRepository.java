package com.example.fashionshop.repository;

import com.example.fashionshop.entity.ProductEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductEmbeddingRepository extends JpaRepository<ProductEmbedding, Long> {
    Optional<ProductEmbedding> findByProductId(Long productId);

}
