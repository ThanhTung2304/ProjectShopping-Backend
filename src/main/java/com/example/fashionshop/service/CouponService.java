package com.example.fashionshop.service;

import com.example.fashionshop.dto.UpdateStatusRequest.coupon.CouponDto;
import com.example.fashionshop.entity.Coupon;

import java.math.BigDecimal;

public interface CouponService {
    CouponDto.ApplyResponse applyCoupon(String code, BigDecimal orderAmount);
    Coupon validateCoupon(String code, BigDecimal orderAmount); // dùng nội bộ trong OrderService
    void incrementUsage(Long couponId);

    // ADMIN
    CouponDto.Response createCoupon(CouponDto.Request request);
    CouponDto.Response updateCoupon(Long id, CouponDto.Request request);
    void deleteCoupon(Long id);
}