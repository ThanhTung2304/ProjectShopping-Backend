// ========================
// RESPONSE: Sau khi đăng nhập / đăng ký thành công
// ========================
// Server trả về: { "token": "eyJ...", "email": "...", "role": "CUSTOMER" }

package com.example.fashionshop.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AuthResponse {
    private String token;    // JWT token → client lưu lại dùng cho các request tiếp theo
    private String email;
    private String fullName;
    private String role;
}
