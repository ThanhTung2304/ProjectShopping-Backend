package com.example.fashionshop.dto.product;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

// ========================
// VariantDto: Biến thể sản phẩm (size + màu + giá + tồn kho)
// ========================
public class VariantDto {

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String size;           // S, M, L, XL
        private String color;          // Trắng, Đen, Xanh...
        private BigDecimal price;      // Giá gốc
        private BigDecimal salePrice;  // Giá khuyến mãi (null nếu không có)
        private Integer stockQuantity; // Tồn kho
        private String sku;            // Mã SKU
        private Boolean isActive;

        // Giá hiển thị thực tế (salePrice nếu có, không thì price)
        public BigDecimal getActualPrice() {
            return salePrice != null ? salePrice : price;
        }

        // Có đang giảm giá không
        public Boolean isOnSale() {
            return salePrice != null;
        }
    }

    @Getter
    public static class Request {

        @NotBlank(message = "Size không được để trống")
        private String size;

        @NotBlank(message = "Màu sắc không được để trống")
        private String color;

        @NotNull(message = "Giá không được để trống")
        @DecimalMin(value = "0", inclusive = false, message = "Giá phải lớn hơn 0")
        private BigDecimal price;

        private BigDecimal salePrice; // Không bắt buộc

        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 0, message = "Số lượng không được âm")
        private Integer stockQuantity;

        @NotBlank(message = "SKU không được để trống")
        private String sku;

        private Long productId;
    }
}


// ========================
// ImageDto: Ảnh sản phẩm
// ========================
class ImageDto {

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String imageUrl;
        private Boolean isPrimary;  // Ảnh đại diện
        private Integer sortOrder;  // Thứ tự hiển thị
    }

    @Getter
    public static class Request {

        @NotBlank(message = "URL ảnh không được để trống")
        private String imageUrl;

        private Boolean isPrimary = false;
        private Integer sortOrder = 0;
        private Long productId;
    }
}