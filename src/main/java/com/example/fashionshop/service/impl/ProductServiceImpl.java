package com.example.fashionshop.service.impl;

import com.example.fashionshop.dto.product.ImageDto;
import com.example.fashionshop.dto.product.ProductDto;
import com.example.fashionshop.dto.product.VariantDto;
import com.example.fashionshop.entity.Category;
import com.example.fashionshop.entity.Product;
import com.example.fashionshop.entity.ProductImage;
import com.example.fashionshop.entity.ProductVariant;
import com.example.fashionshop.exception.AppException;
import com.example.fashionshop.exception.ErrorCode;
import com.example.fashionshop.mapper.ProductImageMapper;
import com.example.fashionshop.mapper.ProductMapper;
import com.example.fashionshop.mapper.VariantMapper;
import com.example.fashionshop.repository.*;
import com.example.fashionshop.service.FileStorageService;
import com.example.fashionshop.service.ProductEmbeddingWorker;
import com.example.fashionshop.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final Set<String> CLOTHING_SIZES = Set.of("S", "M", "L", "XL", "XXL");
    private static final Set<String> FREE_SIZE_VALUES = Set.of("FREE_SIZE", "ONE_SIZE", "FREESIZE");

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final VariantMapper variantMapper;
    private final ProductRepositoryCustom productRepositoryCustom;
    private final FileStorageService fileStorageService;
    private final ProductEmbeddingWorker productEmbeddingWorker; // THÊM MỚI

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDto.Summary> getProducts(String keyword, Long categoryId,
                                                BigDecimal minPrice, BigDecimal maxPrice,
                                                String size, String color,
                                                Pageable pageable) {

        return productRepositoryCustom
                .searchProducts(keyword, categoryId, minPrice, maxPrice, size, color, pageable)
                .map(product -> {

                    ProductDto.Summary summary = productMapper.toSummary(product);

                    BigDecimal min = BigDecimal.ZERO;
                    BigDecimal max = BigDecimal.ZERO;

                    if (product.getVariants() != null && !product.getVariants().isEmpty()) {
                        min = product.getVariants().stream()
                                .map(v -> v.getSalePrice() != null ? v.getSalePrice() : v.getPrice())
                                .min(BigDecimal::compareTo)
                                .orElse(BigDecimal.ZERO);

                        max = product.getVariants().stream()
                                .map(ProductVariant::getPrice)
                                .max(BigDecimal::compareTo)
                                .orElse(BigDecimal.ZERO);
                    }

                    int totalStock = product.getVariants() == null ? 0 : product.getVariants().stream()
                            .filter(variant -> variant.getIsActive() == null || variant.getIsActive())
                            .mapToInt(variant -> variant.getStockQuantity() != null ? variant.getStockQuantity() : 0)
                            .sum();

                    String primaryImageUrl = null;
                    if (product.getImages() != null && !product.getImages().isEmpty()) {
                        primaryImageUrl = product.getImages().stream()
                                .filter(ProductImage::getIsPrimary)
                                .map(ProductImage::getImageUrl)
                                .findFirst()
                                .orElse(null);
                    }

                    return ProductDto.Summary.builder()
                            .id(summary.getId())
                            .name(summary.getName())
                            .slug(summary.getSlug())
                            .categoryName(summary.getCategoryName())
                            .sizeType(summary.getSizeType())
                            .primaryImageUrl(primaryImageUrl)
                            .minPrice(min)
                            .maxPrice(max)
                            .averageRating(reviewRepository.findAverageRatingByProductId(product.getId()))
                            .totalReviews((int) reviewRepository.countByProductId(product.getId()))
                            .totalStock(totalStock)
                            .build();
                });
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDto.Response getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(
                        ErrorCode.PRODUCT_NOT_FOUND,
                        "Không tìm thấy sản phẩm: " + slug
                ));

        return buildProductResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDto.Response getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(
                        ErrorCode.PRODUCT_NOT_FOUND,
                        "Không tìm thấy sản phẩm với id: " + id
                ));

        return buildProductResponse(product);
    }

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
                .sizeType(resolveSizeType(request.getSizeType()))
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        Product savedProduct = productRepository.save(product);

        // Tự động sinh embedding ngay sau khi tạo sản phẩm, không để lỗi này làm hỏng việc tạo sản phẩm
        safeGenerateEmbedding(savedProduct.getId());

        return buildProductResponse(savedProduct);
    }

    @Override
    @Transactional
    public ProductDto.Response updateProduct(Long id, ProductDto.Request request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

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
        product.setSizeType(resolveSizeType(request.getSizeType()));

        if (request.getIsActive() != null) {
            product.setIsActive(request.getIsActive());
        }

        Product savedProduct = productRepository.save(product);

        // Thông tin sản phẩm đã đổi -> sinh lại embedding cho khớp dữ liệu mới
        safeGenerateEmbedding(savedProduct.getId());

        return buildProductResponse(savedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        product.setIsActive(false);
        productRepository.save(product);

        // Sản phẩm bị vô hiệu hóa -> không cần AI gợi ý nữa, xóa embedding tương ứng
        safeDeleteEmbedding(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImageDto.Response> getProductImages(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        return productImageMapper.toResponseList(
                productImageRepository.findByProductIdOrderBySortOrderAsc(productId));
    }

    @Override
    @Transactional
    public ImageDto.Response addProductImage(Long productId, MultipartFile file, Boolean isPrimary, Integer sortOrder) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        boolean shouldBePrimary = Boolean.TRUE.equals(isPrimary)
                || productImageRepository.findByProductIdAndIsPrimaryTrue(productId).isEmpty();

        if (shouldBePrimary) {
            productImageRepository.clearPrimaryByProductId(productId);
        }

        String imageUrl = fileStorageService.storeProductImage(file);
        ProductImage image = ProductImage.builder()
                .product(product)
                .imageUrl(imageUrl)
                .isPrimary(shouldBePrimary)
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .build();

        return productImageMapper.toResponse(productImageRepository.save(image));
    }

    @Override
    @Transactional
    public ImageDto.Response setPrimaryProductImage(Long productId, Long imageId) {
        ProductImage image = productImageRepository.findByIdAndProductId(imageId, productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_IMAGE_NOT_FOUND));

        productImageRepository.clearPrimaryByProductId(productId);
        image.setIsPrimary(true);

        return productImageMapper.toResponse(productImageRepository.save(image));
    }

    @Override
    @Transactional
    public void deleteProductImage(Long productId, Long imageId) {
        ProductImage image = productImageRepository.findByIdAndProductId(imageId, productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_IMAGE_NOT_FOUND));

        boolean wasPrimary = Boolean.TRUE.equals(image.getIsPrimary());
        String imageUrl = image.getImageUrl();

        productImageRepository.delete(image);
        productImageRepository.flush();
        fileStorageService.deleteByPublicUrl(imageUrl);

        if (wasPrimary) {
            productImageRepository.findByProductIdOrderBySortOrderAsc(productId).stream()
                    .findFirst()
                    .ifPresent(nextImage -> {
                        nextImage.setIsPrimary(true);
                        productImageRepository.save(nextImage);
                    });
        }
    }

    @Override
    @Transactional
    public VariantDto.Response addVariant(Long productId, VariantDto.Request request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (variantRepository.existsBySku(request.getSku())) {
            throw new AppException(ErrorCode.VARIANT_SKU_EXISTS);
        }

        String normalizedSize = normalizeAndValidateSize(product, request.getSize());

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .size(normalizedSize)
                .color(request.getColor())
                .price(request.getPrice())
                .salePrice(request.getSalePrice())
                .stockQuantity(request.getStockQuantity())
                .sku(request.getSku())
                .isActive(true)
                .build();

        VariantDto.Response response = variantMapper.toResponse(variantRepository.save(variant));

        // Variant mới ảnh hưởng tới size/màu/giá của sản phẩm -> sinh lại embedding
        safeGenerateEmbedding(productId);

        return response;
    }

    @Override
    @Transactional
    public VariantDto.Response updateVariant(Long variantId, VariantDto.Request request) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new AppException(ErrorCode.VARIANT_NOT_FOUND));

        if (!variant.getSku().equals(request.getSku())
                && variantRepository.existsBySku(request.getSku())) {
            throw new AppException(ErrorCode.VARIANT_SKU_EXISTS);
        }

        String normalizedSize = normalizeAndValidateSize(variant.getProduct(), request.getSize());

        variant.setSize(normalizedSize);
        variant.setColor(request.getColor());
        variant.setPrice(request.getPrice());
        variant.setSalePrice(request.getSalePrice());
        variant.setStockQuantity(request.getStockQuantity());
        variant.setSku(request.getSku());

        VariantDto.Response response = variantMapper.toResponse(variantRepository.save(variant));

        // Giá/tồn kho/màu thay đổi -> sinh lại embedding cho đúng dữ liệu mới
        safeGenerateEmbedding(variant.getProduct().getId());

        return response;
    }

    @Override
    @Transactional
    public void deleteVariant(Long variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new AppException(ErrorCode.VARIANT_NOT_FOUND));

        variant.setIsActive(false);
        variantRepository.save(variant);

        // Variant bị vô hiệu hóa -> sinh lại embedding để phản ánh đúng size/màu còn lại
        safeGenerateEmbedding(variant.getProduct().getId());
    }

    private ProductDto.Response buildProductResponse(Product product) {
        ProductDto.Response response = productMapper.toResponse(product);

        Double avgRating = reviewRepository.findAverageRatingByProductId(product.getId());
        int totalReviews = (int) reviewRepository.countByProductId(product.getId());
        int totalStock = product.getVariants() == null ? 0 : product.getVariants().stream()
                .filter(variant -> variant.getIsActive() == null || variant.getIsActive())
                .mapToInt(variant -> variant.getStockQuantity() != null ? variant.getStockQuantity() : 0)
                .sum();

        return ProductDto.Response.builder()
                .id(response.getId())
                .name(response.getName())
                .slug(response.getSlug())
                .description(response.getDescription())
                .categoryName(response.getCategoryName())
                .sizeType(response.getSizeType())
                .isActive(response.getIsActive())
                .createdAt(response.getCreatedAt())
                .variants(response.getVariants())
                .images(response.getImages())
                .averageRating(avgRating != null ? avgRating : 0.0)
                .totalReviews(totalReviews)
                .totalStock(totalStock)
                .build();
    }

    // ProductServiceImpl
    @Override
    @Transactional(readOnly = true)
    public List<VariantDto.Response> getVariantsByProductId(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return variantRepository.findByProductId(productId).stream()
                .map(variantMapper::toResponse)
                .toList();
    }

    private Product.SizeType resolveSizeType(Product.SizeType sizeType) {
        return sizeType != null ? sizeType : Product.SizeType.CLOTHING;
    }

    private String normalizeAndValidateSize(Product product, String rawSize) {
        String size = rawSize == null ? "" : rawSize.trim();
        if (size.isBlank()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Size khong duoc de trong");
        }

        Product.SizeType sizeType = resolveSizeType(product.getSizeType());
        return switch (sizeType) {
            case CLOTHING -> validateClothingSize(size);
            case PANTS -> validateNumericSize(size, 28, 38, "Size quan phai tu 28 den 38");
            case SHOES -> validateNumericSize(size, 35, 44, "Size giay phai tu 35 den 44");
            case FREE_SIZE -> validateFreeSize(size);
        };
    }

    private String validateClothingSize(String size) {
        String normalized = size.toUpperCase(Locale.ROOT);
        if (!CLOTHING_SIZES.contains(normalized)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Size ao phai la S, M, L, XL hoac XXL");
        }
        return normalized;
    }

    private String validateNumericSize(String size, int min, int max, String message) {
        try {
            int value = Integer.parseInt(size);
            if (value < min || value > max) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, message);
            }
            return String.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, message);
        }
    }

    private String validateFreeSize(String size) {
        String normalized = size.toUpperCase(Locale.ROOT).replace(" ", "_");
        if (!FREE_SIZE_VALUES.contains(normalized)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "Size free size phai la FREE_SIZE hoac ONE_SIZE");
        }
        return "FREE_SIZE";
    }

    /**
     * Sinh/cập nhật embedding cho sản phẩm, không để lỗi Gemini API (rate limit, timeout...)
     * làm hỏng luôn thao tác chính (tạo/sửa sản phẩm vẫn phải thành công).
     */
    private void safeGenerateEmbedding(Long productId) {
        try {
            productEmbeddingWorker.generateAndSaveEmbeddingById(productId);
        } catch (Exception e) {
            log.error("Không thể sinh embedding cho sản phẩm ID {}: {}", productId, e.getMessage());
        }
    }

    private void safeDeleteEmbedding(Long productId) {
        try {
            productEmbeddingWorker.deleteEmbeddingByProductId(productId);
        } catch (Exception e) {
            log.error("Không thể xóa embedding cho sản phẩm ID {}: {}", productId, e.getMessage());
        }
    }
}