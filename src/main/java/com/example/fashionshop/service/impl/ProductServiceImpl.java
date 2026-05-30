package com.example.fashionshop.service.impl;

import com.example.fashionshop.dto.product.ProductDto;
import com.example.fashionshop.dto.product.VariantDto;
import com.example.fashionshop.entity.Category;
import com.example.fashionshop.entity.Product;
import com.example.fashionshop.entity.ProductVariant;
import com.example.fashionshop.exception.AppException;
import com.example.fashionshop.exception.ErrorCode;
import com.example.fashionshop.mapper.ProductMapper;
import com.example.fashionshop.mapper.VariantMapper;
import com.example.fashionshop.repository.*;
import com.example.fashionshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;
    private final ProductMapper productMapper;
    private final VariantMapper variantMapper;
    private final ProductRepositoryCustom productRepositoryCustom;

    // ========================
    // Danh sách sản phẩm (có filter + phân trang)
    // ========================
    @Override
    public Page<ProductDto.Summary> getProducts(String keyword, Long categoryId,
                                                BigDecimal minPrice, BigDecimal maxPrice,
                                                String size, String color, Pageable pageable) {
        return productRepositoryCustom
                .searchProducts(keyword, categoryId, minPrice, maxPrice, size, color, pageable)
                .map(product -> {
                    ProductDto.Summary summary = productMapper.toSummary(product);

                    // Tính giá min/max từ variants
                    var variants = product.getVariants();
                    if (variants != null && !variants.isEmpty()) {
                        BigDecimal min = variants.stream()
                                .map(v -> v.getSalePrice() != null ? v.getSalePrice() : v.getPrice())
                                .min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                        BigDecimal max = variants.stream()
                                .map(v -> v.getPrice())
                                .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

                        // Set qua builder vì Summary dùng @Builder
                        return ProductDto.Summary.builder()
                                .id(summary.getId())
                                .name(summary.getName())
                                .slug(summary.getSlug())
                                .categoryName(summary.getCategoryName())
                                .primaryImageUrl(product.getImages().stream()
                                        .filter(img -> img.getIsPrimary())
                                        .map(img -> img.getImageUrl())
                                        .findFirst().orElse(null))
                                .minPrice(min)
                                .maxPrice(max)
                                .averageRating(reviewRepository.findAverageRatingByProductId(product.getId()))
                                .totalReviews((int) reviewRepository.countByProductId(product.getId()))
                                .build();
                    }
                    return summary;
                });
    }

    // ========================
    // Chi tiết sản phẩm theo slug
    // ========================
    @Override
    public ProductDto.Response getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND,
                        "Không tìm thấy sản phẩm: " + slug));
        return buildProductResponse(product);
    }

    // ========================
    // Chi tiết sản phẩm theo id
    // ========================
    @Override
    public ProductDto.Response getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND,
                        "Không tìm thấy sản phẩm với id: " + id));
        return buildProductResponse(product);
    }

    // ========================
    // Tạo sản phẩm mới (ADMIN)
    // ========================
    @Override
    @Transactional
    public ProductDto.Response createProduct(ProductDto.Request request) {
        if (productRepository.existsBySlug(request.getSlug())) {
            throw new AppException(ErrorCode.PRODUCT_SLUG_EXISTS);
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        Product product = Product.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .description(request.getDescription())
                .category(category)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        return buildProductResponse(productRepository.save(product));
    }

    // ========================
    // Cập nhật sản phẩm (ADMIN)
    // ========================
    @Override
    @Transactional
    public ProductDto.Response updateProduct(Long id, ProductDto.Request request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        // Kiểm tra slug trùng (trừ chính nó)
        if (!product.getSlug().equals(request.getSlug())
                && productRepository.existsBySlug(request.getSlug())) {
            throw new AppException(ErrorCode.PRODUCT_SLUG_EXISTS);
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        product.setName(request.getName());
        product.setSlug(request.getSlug());
        product.setDescription(request.getDescription());
        product.setCategory(category);
        if (request.getIsActive() != null) product.setIsActive(request.getIsActive());

        return buildProductResponse(productRepository.save(product));
    }

    // ========================
    // Xóa sản phẩm (ADMIN)
    // ========================
    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        // Soft delete
        product.setIsActive(false);
        productRepository.save(product);
    }

    // ========================
    // Thêm variant cho sản phẩm (ADMIN)
    // ========================
    @Override
    @Transactional
    public VariantDto.Response addVariant(Long productId, VariantDto.Request request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (variantRepository.existsBySku(request.getSku())) {
            throw new AppException(ErrorCode.VARIANT_SKU_EXISTS);
        }

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .size(request.getSize())
                .color(request.getColor())
                .price(request.getPrice())
                .salePrice(request.getSalePrice())
                .stockQuantity(request.getStockQuantity())
                .sku(request.getSku())
                .isActive(true)
                .build();

        return variantMapper.toResponse(variantRepository.save(variant));
    }

    // ========================
    // Cập nhật variant (ADMIN)
    // ========================
    @Override
    @Transactional
    public VariantDto.Response updateVariant(Long variantId, VariantDto.Request request) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new AppException(ErrorCode.VARIANT_NOT_FOUND));

        // Kiểm tra SKU trùng (trừ chính nó)
        if (!variant.getSku().equals(request.getSku())
                && variantRepository.existsBySku(request.getSku())) {
            throw new AppException(ErrorCode.VARIANT_SKU_EXISTS);
        }

        variant.setSize(request.getSize());
        variant.setColor(request.getColor());
        variant.setPrice(request.getPrice());
        variant.setSalePrice(request.getSalePrice());
        variant.setStockQuantity(request.getStockQuantity());
        variant.setSku(request.getSku());

        return variantMapper.toResponse(variantRepository.save(variant));
    }

    // ========================
    // Xóa variant (ADMIN)
    // ========================
    @Override
    @Transactional
    public void deleteVariant(Long variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new AppException(ErrorCode.VARIANT_NOT_FOUND));
        variant.setIsActive(false);
        variantRepository.save(variant);
    }

    // ========================
    // Helper: Build ProductResponse đầy đủ
    // ========================
    private ProductDto.Response buildProductResponse(Product product) {
        ProductDto.Response response = productMapper.toResponse(product);
        Double avgRating = reviewRepository.findAverageRatingByProductId(product.getId());
        int totalReviews = (int) reviewRepository.countByProductId(product.getId());

        return ProductDto.Response.builder()
                .id(response.getId())
                .name(response.getName())
                .slug(response.getSlug())
                .description(response.getDescription())
                .categoryName(response.getCategoryName())
                .isActive(response.getIsActive())
                .createdAt(response.getCreatedAt())
                .variants(response.getVariants())
                .images(response.getImages())
                .averageRating(avgRating)
                .totalReviews(totalReviews)
                .build();
    }
}