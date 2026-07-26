package com.example.fashionshop.service;

import com.example.fashionshop.dto.product.ProductDto;
import com.example.fashionshop.dto.product.ImageDto;
import com.example.fashionshop.dto.product.VariantDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {
    // ===== Sản phẩm =====
    Page<ProductDto.Summary> getProducts(String keyword, Long categoryId,
                                         BigDecimal minPrice, BigDecimal maxPrice,
                                         String size, String color, Pageable pageable);

    List<ProductDto.Summary> getFeaturedProducts();

    ProductDto.Response getProductBySlug(String slug);
    ProductDto.Response getProductById(Long id);

    // ===== ADMIN =====
    ProductDto.Response createProduct(ProductDto.Request request);
    ProductDto.Response updateProduct(Long id, ProductDto.Request request);
    ProductDto.Response toggleFeatured(Long id, Boolean featured);
    void deleteProduct(Long id);

    // ===== Product images =====
    List<ImageDto.Response> getProductImages(Long productId);
    ImageDto.Response addProductImage(Long productId, MultipartFile file, Boolean isPrimary, Integer sortOrder);
    ImageDto.Response setPrimaryProductImage(Long productId, Long imageId);
    void deleteProductImage(Long productId, Long imageId);

    // ===== Variant =====
    VariantDto.Response addVariant(Long productId, VariantDto.Request request);
    VariantDto.Response updateVariant(Long variantId, VariantDto.Request request);
    void deleteVariant(Long variantId);
    // ProductService interface
    List<VariantDto.Response> getVariantsByProductId(Long productId);
}
