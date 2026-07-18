package com.example.fashionshop.controller;

import com.example.fashionshop.service.ProductEmbeddingService;
import com.example.fashionshop.dto.EmbeddingGenerationResult.EmbeddingGenerationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/embeddings")
@RequiredArgsConstructor
public class ProductEmbeddingController {

    private final ProductEmbeddingService productEmbeddingService;

    @PostMapping("/generate-all")
    public EmbeddingGenerationResult generateAll() {
        return productEmbeddingService.generateEmbeddingsForAllProducts();
    }
}