package com.example.fashionshop.service;

import com.example.fashionshop.dto.EmbeddingGenerationResult.EmbeddingGenerationResult;

public interface ProductEmbeddingService {
    EmbeddingGenerationResult generateEmbeddingsForAllProducts();
}