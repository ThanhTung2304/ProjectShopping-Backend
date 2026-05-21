package com.example.fashionshop.mapper;

import com.example.fashionshop.dto.product.VariantDto;
import com.example.fashionshop.entity.ProductVariant;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VariantMapper {

    // Entity → Response DTO
    // Tất cả field trùng tên → tự map
    VariantDto.Response toResponse(ProductVariant variant);

    List<VariantDto.Response> toResponseList(List<ProductVariant> variants);
}
