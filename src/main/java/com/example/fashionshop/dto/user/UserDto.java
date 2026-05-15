package com.example.fashionshop.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

public class UserDto {

    // ========================
    // RESPONSE: Thông tin user trả về client
    // ========================
    // KHÔNG có password, KHÔNG có cartItems, addresses (tránh lộ dữ liệu nhạy cảm)

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response{
        private Long id;
        private String email;
        private String fullName;
        private String phone;
        private String avatar;
        private String role;
        private Boolean isActive;
        private LocalDateTime createdAt;
    }

    // ========================
    // REQUEST: Cập nhật thông tin cá nhân
    // ========================
    // Dùng khi: PUT /api/users/profile
    // Chỉ cho phép đổi tên và SĐT, KHÔNG cho đổi email/role qua đây
    @Getter
    public static class UpdateRequest{
        @NotBlank(message = "Họ và tên không được để trống")
        private String fullName;

        private String phone;
    }

    // ========================
    // REQUEST: Đổi mật khẩu
    // ========================
    // Dùng khi: PUT /api/users/change-password
    @Getter
    public static class ChangePasswordRequest{
        @NotBlank(message = "Mật khẩu cũ không được để trống")
        private String oldPassword;

        @NotBlank(message = "Mật khẩu mới không được để trống")
        @Size(min = 6, message = "Mật khẩu mới tối thiểu 6 kí tự")
        private String newPassword;
    }
}
