package com.example.fashionshop.mapper;

import com.example.fashionshop.dto.product.ImageDto;
import com.example.fashionshop.entity.ProductImage;
import org.mapstruct.Mapper;

import java.util.List;

//// Tạm dùng VariantDto.ImageDto vì ImageDto nằm trong file VariantDto
//// Nếu tách file riêng thì import ImageDto.Response trực tiếp
//@Mapper(componentModel = "spring")
public interface ProductImageMapper {

    // MapStruct tự map vì tên field giống nhau
    com.example.fashionshop.dto.product.ImageDto.Response toResponse(ProductImage image);

    List<com.example.fashionshop.dto.product.VariantDto.Response> toResponseList(List<ProductImage> images);
}