package com.example.fashionshop.controller;

import com.example.fashionshop.dto.ApiResponse;
import com.example.fashionshop.dto.user.UserDto;
import com.example.fashionshop.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserDto.Response>>> getAllUsers(
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto.Response>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto.Response>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserDto.AdminUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cap nhat tai khoan thanh cong",
                userService.updateUser(id, request)));
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody UserDto.AdminChangePasswordRequest request) {
        userService.adminChangePassword(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Doi mat khau tai khoan thanh cong"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserDto.Response>> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserDto.UpdateStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cap nhat trang thai tai khoan thanh cong",
                userService.updateUserStatus(id, request)));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserDto.Response>> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UserDto.UpdateRoleRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cap nhat quyen tai khoan thanh cong",
                userService.updateUserRole(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.ok("Vo hieu hoa tai khoan thanh cong"));
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<ApiResponse<Void>> hardDeleteUser(@PathVariable Long id) {
        userService.hardDeleteUser(id);
        return ResponseEntity.ok(ApiResponse.ok("Xoa tai khoan vinh vien thanh cong"));
    }
}
