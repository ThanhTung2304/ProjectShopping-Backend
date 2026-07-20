package com.example.fashionshop.service.impl;

import com.example.fashionshop.entity.ProductEmbedding;
import com.example.fashionshop.repository.ProductEmbeddingRepository;
import com.example.fashionshop.service.GeminiEmbeddingService;
import com.example.fashionshop.service.VectorSearchService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VectorSearchServiceImpl implements VectorSearchService {

    private final ProductEmbeddingRepository embeddingRepository;
    private final GeminiEmbeddingService geminiEmbeddingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<ScoredProduct> findRelevantProducts(String userQuery, int topK) {
        List<Double> queryVector = geminiEmbeddingService.embedText(userQuery);

        List<ProductEmbedding> allEmbeddings = embeddingRepository.findAll();

        return allEmbeddings.stream()
                .map(pe -> {
                    List<Double> productVector = parseVector(pe.getEmbeddingVector());
                    double score = cosineSimilarity(queryVector, productVector);
                    return new ScoredProduct(pe.getProduct(), score, pe.getSourceText());
                })
                .sorted(Comparator.comparingDouble(ScoredProduct::score).reversed())
                .limit(topK)
                .toList();
    }

    private List<Double> parseVector(String json) {
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Double.class));
        } catch (Exception e) {
            return List.of();
        }
    }

    private double cosineSimilarity(List<Double> a, List<Double> b) {
        if (a.isEmpty() || b.isEmpty() || a.size() != b.size()) return 0.0;

        double dotProduct = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.size(); i++) {
            dotProduct += a.get(i) * b.get(i);
            normA += Math.pow(a.get(i), 2);
            normB += Math.pow(b.get(i), 2);
        }
        if (normA == 0 || normB == 0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}