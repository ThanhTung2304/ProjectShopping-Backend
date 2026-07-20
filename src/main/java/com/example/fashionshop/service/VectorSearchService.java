package com.example.fashionshop.service;

import com.example.fashionshop.entity.Product;

import java.util.List;

public interface VectorSearchService {

    record ScoredProduct(Product product, double score, String sourceText) {}

    List<ScoredProduct> findRelevantProducts(String userQuery, int topK);
}