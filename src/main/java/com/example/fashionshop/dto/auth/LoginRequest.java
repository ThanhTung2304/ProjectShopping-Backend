// ========================
// REQUEST: Đăng nhập
// ========================
// Dùng khi: POST /api/auth/login
// Client gửi lên: { "email": "...", "password": "..." }

package com.example.fashionshop.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    public String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    public String password;

    public String getEmail() {
        return email;
    }
    public String getPassword() {
        return password;
    }
}
