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

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    // ===== PUBLIC =====

    // GET /api/coupons/active — User xem danh sách coupon đang hoạt động
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<CouponDto.Response>>> getActiveCoupons() {
        return ResponseEntity.ok(ApiResponse.success(couponService.getActiveCoupons()));
    }

    // POST /api/coupons/apply — User nhập mã giảm giá
    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<CouponDto.ApplyResponse>> applyCoupon(
            @Valid @RequestBody CouponDto.ApplyRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                couponService.applyCoupon(request.getCode(), request.getOrderAmount())));
    }

    // ===== ADMIN =====

    // GET /api/coupons — ADMIN xem tất cả coupon
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<CouponDto.Response>>> getAllCoupons() {
        return ResponseEntity.ok(ApiResponse.success(couponService.getAllCoupons()));
    }

    // POST /api/coupons
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CouponDto.Response>> createCoupon(
            @Valid @RequestBody CouponDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo mã giảm giá thành công",
                        couponService.createCoupon(request)));
    }

    // PUT /api/coupons/{id}
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CouponDto.Response>> updateCoupon(
            @PathVariable Long id,
            @Valid @RequestBody CouponDto.Request request) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật mã giảm giá thành công",
                couponService.updateCoupon(id, request)));
    }

    // DELETE /api/coupons/{id}
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCoupon(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa mã giảm giá thành công"));
    }
}