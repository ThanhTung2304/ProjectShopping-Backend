package com.example.fashionshop.dto.EmbeddingGenerationResult;

import java.util.List;

public record EmbeddingGenerationResult(
        int total,
        int successCount,
        int failureCount,
        List<String> failures
) {}