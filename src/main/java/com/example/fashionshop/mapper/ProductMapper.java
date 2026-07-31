package com.example.fashionshop.mapper;

import com.example.fashionshop.dto.product.ProductDto;
import com.example.fashionshop.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(
        componentModel = "spring",
        uses = {
                VariantMapper.class,
                ProductImageMapper.class
        }
)
public interface ProductMapper {

    /**
     * Entity -> Response đầy đủ.
     *
     * productCode được MapStruct tự động map
     * vì Product và ProductDto.Response cùng tên field.
     */
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "variants", source = "variants")
    @Mapping(target = "images", source = "images")
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "totalReviews", ignore = true)
    @Mapping(target = "totalStock", ignore = true)
    ProductDto.Response toResponse(Product product);

    /**
     * Entity -> Summary.
     *
     * productCode cũng được MapStruct tự động map.
     */
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "primaryImageUrl", ignore = true)
    @Mapping(target = "minPrice", ignore = true)
    @Mapping(target = "maxPrice", ignore = true)
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "totalReviews", ignore = true)
    @Mapping(target = "totalStock", ignore = true)
    ProductDto.Summary toSummary(Product product);

    List<ProductDto.Summary> toSummaryList(List<Product> products);
}