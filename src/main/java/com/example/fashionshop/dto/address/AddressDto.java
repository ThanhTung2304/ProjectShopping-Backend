package com.example.fashionshop.dto.address;

import lombok.*;
import jakarta.validation.constraints.NotBlank;

public class AddressDto {
    // ========================
    // RESPONSE: Thông tin địa chỉ trả về
    // ========================
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response{
        private Long id;
        private String recipientName;
        private String phone;
        private String province;
        private String district;
        private String ward;
        private String detail;
        private Boolean isDefault;

        // Địa chỉ đầy đủ dạng chuỗi (tiện hiển thị)
        public String getFullAddress() {
            return detail + ", " + ward + ", " + district + ", " + province;
        }
    }

    // ========================
    // REQUEST: Thêm / Sửa địa chỉ
    // ========================
    // Dùng khi: POST /api/addresses  hoặc  PUT /api/addresses/{id}
    @Getter
    public static class Request{
        @NotBlank(message = "Họ tên không được để trống")
        private String fullName;

        @NotBlank(message = "Số điện thoại không được để trống")
        private String phone;

        @NotBlank(message = "Tỉnh/Thành phố không được để trống")
        private String province;

        @NotBlank(message = "Quận/Huyện không được để trống")
        private String district;

        @NotBlank(message = "Phường/Xã không được để trống")
        private String ward;

        @NotBlank(message = "Địa chỉ chi tiết không được để trống")
        private String detail;

        private Boolean isDefault = false;

    }
}
