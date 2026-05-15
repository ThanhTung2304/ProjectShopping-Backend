package com.example.fashionshop.dto.UpdateStatusRequest.review;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

// ========================
// ReviewDto: Đánh giá sản phẩm
// ========================
public class ReviewDto {

    // RESPONSE: Thông tin 1 đánh giá
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String userFullName; // Tên người đánh giá
        private Byte rating;         // 1 → 5 sao
        private String comment;      // Nội dung đánh giá
        private LocalDateTime createdAt;
    }

    // RESPONSE: Tổng hợp đánh giá của sản phẩm
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Summary {
        private Double averageRating;         // Điểm TB (vd: 4.5)
        private Integer totalReviews;         // Tổng số đánh giá
        private java.util.Map<Integer, Long> ratingDistribution;
        // Phân bổ: {5: 50, 4: 30, 3: 10, 2: 5, 1: 5}
    }

    // REQUEST: Viết đánh giá
    // Dùng khi: POST /api/reviews
    @Getter
    public static class Request {

        @NotNull(message = "Sản phẩm không được để trống")
        private Long productId;

        @NotNull(message = "Đơn hàng không được để trống")
        private Long orderId;

        @NotNull(message = "Số sao không được để trống")
        @Min(value = 1, message = "Số sao tối thiểu là 1")
        @Max(value = 5, message = "Số sao tối đa là 5")
        private Byte rating;

        private String comment;
    }
}