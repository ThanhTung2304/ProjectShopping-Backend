package com.example.fashionshop.controller;

import com.example.fashionshop.dto.ApiResponse;
import com.example.fashionshop.dto.address.AddressDto;
import com.example.fashionshop.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    // GET /api/addresses
    // Lấy danh sách địa chỉ của user
    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressDto.Response>>> getAddresses(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.success(addressService.getAddresses(userDetails.getUsername())));
    }

    // POST /api/addresses
    // Thêm địa chỉ mới
    @PostMapping
    public ResponseEntity<ApiResponse<AddressDto.Response>> addAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AddressDto.Request request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm địa chỉ thành công",
                        addressService.addAddress(userDetails.getUsername(), request)));
    }

    // PUT /api/addresses/{id}
    // Cập nhật địa chỉ
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressDto.Response>> updateAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody AddressDto.Request request) {
        return ResponseEntity.ok(
                ApiResponse.success("Cập nhật địa chỉ thành công",
                        addressService.updateAddress(userDetails.getUsername(), id, request)));
    }

    // DELETE /api/addresses/{id}
    // Xóa địa chỉ
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        addressService.deleteAddress(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa địa chỉ thành công"));
    }

    // PATCH /api/addresses/{id}/default
    // Đặt địa chỉ mặc định
    @PatchMapping("/{id}/default")
    public ResponseEntity<ApiResponse<AddressDto.Response>> setDefault(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success("Đặt địa chỉ mặc định thành công",
                        addressService.setDefault(userDetails.getUsername(), id)));
    }
}