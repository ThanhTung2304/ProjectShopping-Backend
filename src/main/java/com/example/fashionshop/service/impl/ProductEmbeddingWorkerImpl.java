package com.example.fashionshop.service.impl;
import com.example.fashionshop.entity.Product;
import com.example.fashionshop.entity.ProductEmbedding;
import com.example.fashionshop.entity.ProductVariant;
import com.example.fashionshop.repository.ProductEmbeddingRepository;
import com.example.fashionshop.repository.ProductRepository;
import com.example.fashionshop.service.GeminiEmbeddingService;
import com.example.fashionshop.service.ProductEmbeddingWorker;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProductEmbeddingWorkerImpl implements ProductEmbeddingWorker {

    private final ProductRepository productRepository;
    private final ProductEmbeddingRepository embeddingRepository;
    private final GeminiEmbeddingService geminiEmbeddingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public void generateAndSaveEmbeddingById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID " + productId));

        String sourceText = buildSourceText(product);
        List<Double> vector = geminiEmbeddingService.embedText(sourceText);

        try {
            String vectorJson = objectMapper.writeValueAsString(vector);

            ProductEmbedding embedding = embeddingRepository.findByProductId(product.getId())
                    .orElse(ProductEmbedding.builder().product(product).build());

            embedding.setEmbeddingVector(vectorJson);
            embedding.setSourceText(sourceText);
            embeddingRepository.save(embedding);

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lưu embedding: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void deleteEmbeddingByProductId(Long productId) {
        embeddingRepository.findByProductId(productId)
                .ifPresent(embeddingRepository::delete);
    }

    private String buildSourceText(Product product) {
        StringBuilder sb = new StringBuilder();

        sb.append("Tên sản phẩm: ").append(product.getName()).append(". ");

        if (product.getCategory() != null) {
            sb.append("Danh mục: ").append(product.getCategory().getName()).append(". ");
        }

        if (product.getDescription() != null && !product.getDescription().isBlank()) {
            sb.append("Mô tả: ").append(product.getDescription()).append(". ");
        }

        sb.append("Loại size: ").append(mapSizeType(product.getSizeType())).append(". ");

        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            List<String> sizes = product.getVariants().stream()
                    .map(ProductVariant::getSize)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            List<String> colors = product.getVariants().stream()
                    .map(ProductVariant::getColor)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            // Lỗi 2 đã sửa: cả minPrice và maxPrice đều ưu tiên salePrice trước
            BigDecimal minPrice = product.getVariants().stream()
                    .map(v -> v.getSalePrice() != null ? v.getSalePrice() : v.getPrice())
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(null);

            BigDecimal maxPrice = product.getVariants().stream()
                    .map(v -> v.getSalePrice() != null ? v.getSalePrice() : v.getPrice())
                    .filter(Objects::nonNull)
                    .max(BigDecimal::compareTo)
                    .orElse(null);

            if (!sizes.isEmpty()) {
                sb.append("Size có sẵn: ").append(String.join(", ", sizes)).append(". ");
            }
            if (!colors.isEmpty()) {
                sb.append("Màu sắc: ").append(String.join(", ", colors)).append(". ");
            }
            if (minPrice != null && maxPrice != null) {
                sb.append("Giá: từ ").append(minPrice).append(" đến ").append(maxPrice).append(" VNĐ. ");
            }
        }

        return sb.toString();
    }

    private String mapSizeType(Product.SizeType sizeType) {
        if (sizeType == null) return "";
        return switch (sizeType) {
            case CLOTHING -> "quần áo";
            case PANTS -> "quần";
            case SHOES -> "giày";
            case FREE_SIZE -> "size tự do";
        };
    }
}