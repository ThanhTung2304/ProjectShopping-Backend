package com.example.fashionshop.mapper;

import com.example.fashionshop.dto.product.ProductDto;
import com.example.fashionshop.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {VariantMapper.class, ProductImageMapper.class})
// uses = {} → dùng VariantMapper và ProductImageMapper để convert relations
public interface ProductMapper {

    // Entity → Response đầy đủ (kèm variants + images)
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "variants",     source = "variants")   // → dùng VariantMapper
    @Mapping(target = "images",       source = "images")     // → dùng ProductImageMapper
    @Mapping(target = "averageRating", ignore = true)        // tính riêng trong Service
    @Mapping(target = "totalReviews",  ignore = true)        // tính riêng trong Service
    ProductDto.Response toResponse(Product product);

    // Entity → Summary (chỉ thông tin cơ bản, dùng trong danh sách)
    @Mapping(target = "categoryName",    source = "category.name")
    @Mapping(target = "primaryImageUrl", ignore = true)  // xử lý trong Service
    @Mapping(target = "minPrice",        ignore = true)  // tính từ variants trong Service
    @Mapping(target = "maxPrice",        ignore = true)  // tính từ variants trong Service
    @Mapping(target = "averageRating",   ignore = true)
    @Mapping(target = "totalReviews",    ignore = true)
    ProductDto.Summary toSummary(Product product);

    List<ProductDto.Summary> toSummaryList(List<Product> products);
}