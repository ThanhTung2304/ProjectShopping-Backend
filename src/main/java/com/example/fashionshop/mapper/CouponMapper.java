package com.example.fashionshop.mapper;

import com.example.fashionshop.dto.UpdateStatusRequest.coupon.CouponDto;
import com.example.fashionshop.entity.Coupon;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CouponMapper {

    // Entity → Response DTO
    // Tất cả field trùng tên → tự map
    CouponDto.Response toResponse(Coupon coupon);
}
