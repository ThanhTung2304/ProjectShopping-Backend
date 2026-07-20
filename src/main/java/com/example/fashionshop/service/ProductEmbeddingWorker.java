package com.example.fashionshop.service;

public interface ProductEmbeddingWorker {
    void generateAndSaveEmbeddingById(Long productId);

    void deleteEmbeddingByProductId(Long productId);
}