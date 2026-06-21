package com.example.fashionshop.controller;

import com.example.fashionshop.dto.ApiResponse;
import com.example.fashionshop.dto.auth.*;
import com.example.fashionshop.service.AuthService;
import com.example.fashionshop.service.OtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    // ========================
    // POST /api/auth/register
    // ========================
    // Body: { "fullName": "...", "email": "...", "password": "...", "phone": "..." }
    // Response: { "success": true, "data": { "token": "...", "email": "...", ... } }
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse data = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đăng ký thành công", data));
    }

    // ========================
    // POST /api/auth/login
    // ========================
    // Body: { "email": "...", "password": "..." }
    // Response: { "success": true, "data": { "token": "...", "email": "...", ... } }
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse data = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", data));
    }

    // ========================
    // POST /api/auth/forgot-password
    // ========================
    // Body: { "email": "user@gmail.com" }
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
            @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail(), otpService);
        return ResponseEntity.ok(ApiResponse.success("OTP đã được gửi về email của bạn", null));
    }

    // ========================
    // POST /api/auth/verify-otp
    // ========================
    // Body: { "email": "...", "otp": "123456" }
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<String>> verifyOtp(
            @RequestBody VerifyOtpRequest request) {
        boolean valid = otpService.verifyOtp(request.getEmail(), request.getOtp());
        if (!valid) {
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error("OTP không hợp lệ hoặc đã hết hạn"));
        }
        return ResponseEntity.ok(ApiResponse.success("OTP hợp lệ", null));
    }

    // ========================
    // POST /api/auth/reset-password
    // ========================
    // Body: { "email": "...", "otp": "123456", "newPassword": "..." }
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request, otpService);
        return ResponseEntity.ok(ApiResponse.success("Đặt lại mật khẩu thành công", null));
    }

    // Thêm vào sau method login()

    // ========================
// POST /api/auth/refresh-token
// ========================
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        RefreshTokenResponse data = authService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Làm mới token thành công", data));
    }

    // ========================
// POST /api/auth/logout
// ========================
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @RequestBody RefreshTokenRequest request) {

        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công", null));
    }
}