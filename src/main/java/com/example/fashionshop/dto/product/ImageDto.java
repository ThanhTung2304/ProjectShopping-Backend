package com.example.fashionshop.dto.product;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// ========================
// ImageDto: Ảnh sản phẩm
// ========================
public class ImageDto {

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
