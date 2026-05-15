package com.example.fashionshop.controller;

import com.example.fashionshop.dto.ApiResponse;
import com.example.fashionshop.dto.auth.AuthResponse;
import com.example.fashionshop.dto.auth.LoginRequest;
import com.example.fashionshop.dto.auth.RegisterRequest;
import com.example.fashionshop.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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
}