package com.example.fashionshop.controller;

import com.example.fashionshop.dto.ApiResponse;
import com.example.fashionshop.dto.user.UserDto;
import com.example.fashionshop.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // GET /api/users/profile
    // Lấy thông tin cá nhân của user đang đăng nhập
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserDto.Response>> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.success(userService.getProfile(userDetails.getUsername())));
    }

    // PUT /api/users/profile
    // Cập nhật thông tin cá nhân
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserDto.Response>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserDto.UpdateRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Cập nhật thành công",
                        userService.updateProfile(userDetails.getUsername(), request)));
    }

    // PUT /api/users/change-password
    // Đổi mật khẩu
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserDto.ChangePasswordRequest request) {
        userService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok("Đổi mật khẩu thành công"));
    }
}