package com.example.fashionshop.dto.coupon;

import com.example.fashionshop.entity.Coupon.DiscountType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CouponDto {

    // ========================
    // RESPONSE: Thông tin mã giảm giá
    // ========================
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String code;
        private DiscountType discountType;   // PERCENT hoặc FIXED
        private BigDecimal discountValue;    // Giá trị giảm
        private BigDecimal minOrderValue;    // Đơn tối thiểu
        private BigDecimal maxDiscount;      // Giảm tối đa (nếu PERCENT)
        private Integer usageLimit;          // Giới hạn lượt dùng
        private Integer usedCount;           // Đã dùng bao nhiêu lượt
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean isActive;
    }

    // ========================
    // RESPONSE: Kết quả áp dụng mã giảm giá
    // ========================
    // Dùng khi user nhập mã → hiển thị số tiền được giảm trước khi đặt hàng
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ApplyResponse {
        private String code;
        private BigDecimal discountAmount;   // Số tiền được giảm
        private BigDecimal finalAmount;      // Tổng tiền sau giảm
        private String message;             // "Áp dụng thành công! Giảm 50,000đ"
    }

    // ========================
    // REQUEST: Tạo mã giảm giá (ADMIN)
    // ========================
    @Getter
    public static class Request {

        @NotBlank(message = "Mã giảm giá không được để trống")
        private String code;

        @NotNull(message = "Loại giảm giá không được để trống")
        private DiscountType discountType;

        @NotNull(message = "Giá trị giảm không được để trống")
        @DecimalMin(value = "0", inclusive = false, message = "Giá trị giảm phải lớn hơn 0")
        private BigDecimal discountValue;

        private BigDecimal minOrderValue = BigDecimal.ZERO;
        private BigDecimal maxDiscount;  // Chỉ dùng khi PERCENT

        @NotNull(message = "Giới hạn lượt dùng không được để trống")
        @Min(value = 1, message = "Giới hạn lượt dùng tối thiểu là 1")
        private Integer usageLimit;

        @NotNull(message = "Ngày bắt đầu không được để trống")
        private LocalDate startDate;

        @NotNull(message = "Ngày kết thúc không được để trống")
        private LocalDate endDate;
    }

    // ========================
    // REQUEST: User nhập mã để kiểm tra
    // ========================
    @Getter
    public static class ApplyRequest {

        @NotBlank(message = "Mã giảm giá không được để trống")
        private String code;

        @NotNull(message = "Tổng tiền đơn hàng không được để trống")
        private BigDecimal orderAmount;
    }
}