package com.example.fashionshop.service.impl;

import com.example.fashionshop.dto.UpdateStatusRequest.coupon.CouponDto;
import com.example.fashionshop.entity.Coupon;
import com.example.fashionshop.exception.AppException;
import com.example.fashionshop.exception.ErrorCode;
import com.example.fashionshop.mapper.CouponMapper;
import com.example.fashionshop.repository.CouponRepository;
import com.example.fashionshop.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final CouponMapper couponMapper;

    // ========================
    // Kiểm tra và áp dụng mã giảm giá
    // ========================
    @Override
    public CouponDto.ApplyResponse applyCoupon(String code, BigDecimal orderAmount) {
        Coupon coupon = validateCoupon(code, orderAmount);
        BigDecimal discount = calculateDiscount(coupon, orderAmount);
        BigDecimal finalAmount = orderAmount.subtract(discount);

        return CouponDto.ApplyResponse.builder()
                .code(coupon.getCode())
                .discountAmount(discount)
                .finalAmount(finalAmount)
                .message("Áp dụng thành công! Giảm " + discount + "đ")
                .build();
    }

    // ========================
    // Validate coupon (dùng nội bộ)
    // ========================
    @Override
    public Coupon validateCoupon(String code, BigDecimal orderAmount) {
        Coupon coupon = couponRepository.findValidCoupon(code, LocalDate.now())
                .orElseThrow(() -> new AppException(ErrorCode.COUPON_INVALID));

        // Kiểm tra đơn tối thiểu
        if (orderAmount.compareTo(coupon.getMinOrderValue()) < 0) {
            throw new AppException(ErrorCode.COUPON_MIN_ORDER_NOT_MET);
        }

        return coupon;
    }

    // ========================
    // Tăng lượt dùng
    // ========================
    @Override
    @Transactional
    public void incrementUsage(Long couponId) {
        couponRepository.incrementUsedCount(couponId);
    }

    @Override
    public List<CouponDto.Response> getAllCoupons() {
        return couponRepository.findAll()
                .stream()
                .map(couponMapper::toResponse)
                .toList();
    }

    // ========================
    // Tạo mã giảm giá (ADMIN)
    // ========================
    @Override
    @Transactional
    public CouponDto.Response createCoupon(CouponDto.Request request) {
        if (couponRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.COUPON_CODE_EXISTS);
        }

        Coupon coupon = Coupon.builder()
                .code(request.getCode().toUpperCase())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minOrderValue(request.getMinOrderValue() != null
                        ? request.getMinOrderValue() : BigDecimal.ZERO)
                .maxDiscount(request.getMaxDiscount())
                .usageLimit(request.getUsageLimit())
                .usedCount(0)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isActive(true)
                .build();

        return couponMapper.toResponse(couponRepository.save(coupon));
    }

    // ========================
    // Cập nhật mã giảm giá (ADMIN)
    // ========================
    @Override
    @Transactional
    public CouponDto.Response updateCoupon(Long id, CouponDto.Request request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COUPON_NOT_FOUND));

        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinOrderValue(request.getMinOrderValue() != null
                ? request.getMinOrderValue() : BigDecimal.ZERO);
        coupon.setMaxDiscount(request.getMaxDiscount());
        coupon.setUsageLimit(request.getUsageLimit());
        coupon.setStartDate(request.getStartDate());
        coupon.setEndDate(request.getEndDate());

        return couponMapper.toResponse(couponRepository.save(coupon));
    }

    // ========================
    // Xóa mã giảm giá (ADMIN)
    // ========================
    @Override
    @Transactional
    public void deleteCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COUPON_NOT_FOUND));
        coupon.setIsActive(false);
        couponRepository.save(coupon);
    }

    // ========================
    // Helper: Tính số tiền giảm
    // ========================
    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal orderAmount) {
        BigDecimal discount;

        if (coupon.getDiscountType() == Coupon.DiscountType.PERCENT) {
            discount = orderAmount.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100));
            // Giới hạn tối đa nếu có
            if (coupon.getMaxDiscount() != null
                    && discount.compareTo(coupon.getMaxDiscount()) > 0) {
                discount = coupon.getMaxDiscount();
            }
        } else {
            discount = coupon.getDiscountValue();
        }

        // Giảm không được vượt quá tổng đơn
        return discount.compareTo(orderAmount) > 0 ? orderAmount : discount;
    }
}
