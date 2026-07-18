package com.example.fashionshop.service.impl;

import com.example.fashionshop.repository.ProductRepository;
import com.example.fashionshop.entity.Product;
import com.example.fashionshop.service.ProductEmbeddingService;
import com.example.fashionshop.service.ProductEmbeddingWorker;
import com.example.fashionshop.dto.EmbeddingGenerationResult.EmbeddingGenerationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductEmbeddingServiceImpl implements ProductEmbeddingService {

    private final ProductRepository productRepository;
    private final ProductEmbeddingWorker productEmbeddingWorker; // gọi qua bean KHÁC -> proxy hoạt động đúng

    @Override
    public EmbeddingGenerationResult generateEmbeddingsForAllProducts() {
        List<Long> productIds = productRepository.findAll()
                .stream()
                .map(Product::getId)
                .toList();

        int successCount = 0;
        List<String> failures = new ArrayList<>();

        for (Long id : productIds) {
            try {
                productEmbeddingWorker.generateAndSaveEmbeddingById(id); // gọi bean khác -> có transaction thật
                successCount++;
            } catch (Exception e) {
                failures.add("Product ID " + id + ": " + e.getMessage());
            }
        }

        return new EmbeddingGenerationResult(
                productIds.size(),
                successCount,
                failures.size(),
                failures
        );
    }
}