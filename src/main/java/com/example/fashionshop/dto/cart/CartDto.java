package com.example.fashionshop.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

public class CartDto {

    // ========================
    // RESPONSE: 1 item trong giỏ hàng
    // ========================
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ItemResponse {
        private Long id;              // CartItem id
        private Long variantId;       // Variant id
        private String productName;   // Tên sản phẩm
        private String size;          // Size đã chọn
        private String color;         // Màu đã chọn
        private String imageUrl;      // Ảnh sản phẩm
        private BigDecimal unitPrice; // Giá 1 sản phẩm (đã tính sale nếu có)
        private Integer quantity;     // Số lượng
        private BigDecimal subtotal;  // unitPrice × quantity
        private Integer stockQuantity;// Tồn kho còn lại (để frontend disable nút +)
    }

    // ========================
    // RESPONSE: Toàn bộ giỏ hàng
    // ========================
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private List<ItemResponse> items;  // Danh sách sản phẩm trong giỏ
        private Integer totalItems;        // Tổng số loại sản phẩm
        private Integer totalQuantity;     // Tổng số lượng
        private BigDecimal totalAmount;    // Tổng tiền
    }

    // ========================
    // REQUEST: Thêm vào giỏ hàng
    // ========================
    // Dùng khi: POST /api/cart
    // { "variantId": 1, "quantity": 2 }
    @Getter
    public static class AddRequest {

        @NotNull(message = "Biến thể sản phẩm không được để trống")
        private Long variantId;

        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 1, message = "Số lượng tối thiểu là 1")
        private Integer quantity;
    }

    // ========================
    // REQUEST: Cập nhật số lượng
    // ========================
    // Dùng khi: PUT /api/cart/{id}
    // { "quantity": 3 }
    @Getter
    public static class UpdateRequest {

        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 1, message = "Số lượng tối thiểu là 1")
        private Integer quantity;
    }
}