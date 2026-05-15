package com.example.fashionshop.dto.order;

import com.example.fashionshop.entity.Order.OrderStatus;
import com.example.fashionshop.entity.Payment.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderDto {

    // ========================
    // RESPONSE: Chi tiết 1 sản phẩm trong đơn hàng
    // ========================
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ItemResponse {
        private Long id;
        private String productName;  // Snapshot tên lúc đặt
        private String size;
        private String color;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }

    // ========================
    // RESPONSE: Thông tin đơn hàng đầy đủ
    // ========================
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String orderCode;        // ORD-20240501-001
        private OrderStatus status;      // PENDING, CONFIRMED...
        private BigDecimal totalAmount;
        private BigDecimal shippingFee;
        private BigDecimal discountAmount;
        private BigDecimal finalAmount;
        private String shippingName;
        private String shippingPhone;
        private String shippingAddress;
        private String note;
        private LocalDateTime orderedAt;
        private String paymentMethod;    // COD, MOMO, VNPAY...
        private String paymentStatus;    // PENDING, PAID...
        private List<ItemResponse> items;// Danh sách sản phẩm
    }

    // ========================
    // RESPONSE: Tóm tắt đơn hàng (dùng trong danh sách)
    // ========================
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Summary {
        private Long id;
        private String orderCode;
        private OrderStatus status;
        private BigDecimal finalAmount;
        private Integer totalItems;      // Tổng số loại sản phẩm
        private LocalDateTime orderedAt;
        private String paymentMethod;
        private String paymentStatus;
    }

    // ========================
    // REQUEST: Đặt hàng
    // ========================
    // Client gửi lên: địa chỉ giao, phương thức thanh toán, mã giảm giá (nếu có)
    @Getter
    public static class PlaceOrderRequest {

        @NotNull(message = "Địa chỉ giao hàng không được để trống")
        private Long addressId;          // Chọn từ địa chỉ đã lưu

        @NotNull(message = "Phương thức thanh toán không được để trống")
        private PaymentMethod paymentMethod;

        private String couponCode;       // Mã giảm giá (không bắt buộc)
        private String note;             // Ghi chú đơn hàng
    }

    // ========================
    // REQUEST: Admin cập nhật trạng thái đơn hàng
    // ========================
    @Getter
    public static class UpdateStatusRequest {

        @NotNull(message = "Trạng thái không được để trống")
        private OrderStatus status;
    }
}