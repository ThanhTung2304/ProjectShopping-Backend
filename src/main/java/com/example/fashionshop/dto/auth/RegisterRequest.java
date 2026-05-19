// ========================
// REQUEST: Đăng ký
// ========================
// Dùng khi: POST /api/auth/register
// Client gửi lên: { "fullName": "...", "email": "...", "password": "..." }

package com.example.fashionshop.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

public class RegisterRequest {

    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu tối thiểu 6 kí tự")
    private String password;

    private String phone;

    //Getters, Setters (Lombok)
    public String getFullName() {
        return fullName;
    }
    public String getEmail(){
        return email;
    }
    public String getPassword(){
        return password;
    }
    public String getPhone(){
        return phone;
    }
}
