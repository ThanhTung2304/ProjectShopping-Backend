package com.example.fashionshop.dto.product;

import com.example.fashionshop.entity.Product;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class ProductDto {

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String name;
        private String slug;
        private String description;
        private String categoryName;
        private Product.SizeType sizeType;
        private Boolean isActive;
        private LocalDateTime createdAt;
        private List<VariantDto.Response> variants;
        private List<ImageDto.Response> images;
        private Double averageRating;
        private Integer totalReviews;
        private Integer totalStock;

        public BigDecimal getMinPrice() {
            if (variants == null || variants.isEmpty()) {
                return BigDecimal.ZERO;
            }
            return variants.stream()
                    .map(v -> v.getSalePrice() != null ? v.getSalePrice() : v.getPrice())
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Summary {
        private Long id;
        private String name;
        private String slug;
        private String categoryName;
        private Product.SizeType sizeType;
        private String primaryImageUrl;
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private Double averageRating;
        private Integer totalReviews;
        private Integer totalStock;
    }

    @Getter
    public static class Request {

        @NotBlank(message = "Ten san pham khong duoc de trong")
        private String name;

        @NotBlank(message = "Slug khong duoc de trong")
        private String slug;

        private String description;

        @NotNull(message = "Danh muc khong duoc de trong")
        private Long categoryId;

        private Product.SizeType sizeType = Product.SizeType.CLOTHING;

        private Boolean isActive = true;
    }
}
