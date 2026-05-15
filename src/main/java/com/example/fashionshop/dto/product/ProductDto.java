package com.example.fashionshop.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ProductDto {

    // ========================
    // RESPONSE: Thông tin sản phẩm đầy đủ
    // ========================
    // Dùng khi xem chi tiết sản phẩm
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String name;
        private String slug;
        private String description;
        private String categoryName;
        private Boolean isActive;
        private LocalDateTime createdAt;

        private List<VariantDto.Response> variants;  // Các biến thể (size, màu, giá)
        private List<ImageDto.Response> images;      // Ảnh sản phẩm
        private Double averageRating;                // Điểm đánh giá trung bình
        private Integer totalReviews;                // Tổng số đánh giá

        // Giá thấp nhất trong các variant (tiện hiển thị)
        public BigDecimal getMinPrice() {
            if (variants == null || variants.isEmpty()) return BigDecimal.ZERO;
            return variants.stream()
                    .map(v -> v.getSalePrice() != null ? v.getSalePrice() : v.getPrice())
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
        }
    }

    // ========================
    // RESPONSE: Thông tin sản phẩm tóm tắt (dùng trong danh sách)
    // ========================
    // Dùng khi hiển thị danh sách sản phẩm → chỉ cần thông tin cơ bản
    // KHÔNG cần trả về variants, description để giảm dữ liệu
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Summary {
        private Long id;
        private String name;
        private String slug;
        private String categoryName;
        private String primaryImageUrl;  // Ảnh đại diện
        private BigDecimal minPrice;     // Giá thấp nhất
        private BigDecimal maxPrice;     // Giá cao nhất
        private Double averageRating;
        private Integer totalReviews;
    }

    // ========================
    // REQUEST: Thêm / Sửa sản phẩm (ADMIN)
    // ========================
    @Getter
    public static class Request {

        @NotBlank(message = "Tên sản phẩm không được để trống")
        private String name;

        @NotBlank(message = "Slug không được để trống")
        private String slug;

        private String description;

        @NotNull(message = "Danh mục không được để trống")
        private Long categoryId;

        private Boolean isActive = true;
    }
}