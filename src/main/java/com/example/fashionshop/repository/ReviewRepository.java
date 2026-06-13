package com.example.fashionshop.repository;

import com.example.fashionshop.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByProductId(Long productId, Pageable pageable);

    // Kiểm tra user đã review sản phẩm trong đơn hàng này chưa
    boolean existsByUserIdAndProductIdAndOrderId(Long userId,Long productId, Long orderId);

    boolean existsByUserId(Long userId);

    // Tính rating trung bình của sản phẩm
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Double findAverageRatingByProductId(Long productId);

    // Đếm số lượng review theo từng sao
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.product.id = :productId GROUP BY r.rating ORDER BY r.rating DESC")
    java.util.List<Object[]> countReviewsByRating(Long productId);

    // Đếm tổng số review của sản phẩm
    long countByProductId(Long productId);
}
