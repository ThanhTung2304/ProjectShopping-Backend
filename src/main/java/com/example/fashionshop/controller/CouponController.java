package com.example.fashionshop.controller;

import com.example.fashionshop.dto.ApiResponse;
import com.example.fashionshop.dto.UpdateStatusRequest.coupon.CouponDto;
import com.example.fashionshop.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    // POST /api/coupons/apply
    // User nhập mã giảm giá → kiểm tra và tính số tiền giảm
    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<CouponDto.ApplyResponse>> applyCoupon(
            @Valid @RequestBody CouponDto.ApplyRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                couponService.applyCoupon(request.getCode(), request.getOrderAmount())));
    }

    // ===== ADMIN =====

    // POST /api/coupons — ADMIN only
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CouponDto.Response>> createCoupon(
            @Valid @RequestBody CouponDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo mã giảm giá thành công",
                        couponService.createCoupon(request)));
    }

    // PUT /api/coupons/{id} — ADMIN only
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CouponDto.Response>> updateCoupon(
            @PathVariable Long id,
            @Valid @RequestBody CouponDto.Request request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật mã giảm giá thành công",
                couponService.updateCoupon(id, request)));
    }

    // DELETE /api/coupons/{id} — ADMIN only
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCoupon(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa mã giảm giá thành công"));
    }
}