package com.example.fashionshop.service;

import com.example.fashionshop.dto.product.ProductDto;
import com.example.fashionshop.dto.product.VariantDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface ProductService {
    // ===== Sản phẩm =====
    Page<ProductDto.Summary> getProducts(String keyword, Long categoryId,
                                         BigDecimal minPrice, BigDecimal maxPrice,
                                         String size, String color, Pageable pageable);
    ProductDto.Response getProductBySlug(String slug);
    ProductDto.Response getProductById(Long id);

    // ===== ADMIN =====
    ProductDto.Response createProduct(ProductDto.Request request);
    ProductDto.Response updateProduct(Long id, ProductDto.Request request);
    void deleteProduct(Long id);

    // ===== Variant =====
    VariantDto.Response addVariant(Long productId, VariantDto.Request request);
    VariantDto.Response updateVariant(Long variantId, VariantDto.Request request);
    void deleteVariant(Long variantId);
}