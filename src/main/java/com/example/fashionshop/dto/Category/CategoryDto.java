package com.example.fashionshop.dto.Category;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class CategoryDto {

    // ========================
    // RESPONSE: Thông tin danh mục (đơn giản)
    // ========================
    // Dùng khi hiển thị danh sách sản phẩm, breadcrumb
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String name;
        private String slug;
        private String imageUrl;
        private Boolean isActive;
        private Long parentId; // ID danh mục cha (null nếu là danh mục gốc)
        private String parentName; // Tên danh mục cha
    }

    // ========================
    // RESPONSE: Danh mục kèm danh mục con
    // ========================
    // Dùng khi hiển thị menu navigation
    // Ví dụ: "Áo" → ["Áo thun", "Áo sơ mi", "Áo khoác"]
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ResponseWithChildren {
        private Long id;
        private String name;
        private String slug;
        private String imageUrl;
        private List<Response> children;
    }

    // ========================
    // REQUEST: Thêm / Sửa danh mục
    // ========================
    @Getter
    public static class Request {
        @NotBlank(message = "Tên danh mục khng được để trống")
        private String name;

        @NotBlank(message = "Slug không được để trống")
        private String slug;

        private Long parentId;
        private String imageUrl;
        private Boolean isActive = true;
    }
}
