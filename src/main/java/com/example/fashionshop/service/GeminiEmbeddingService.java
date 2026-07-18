package com.example.fashionshop.service;

import java.util.List;

public interface GeminiEmbeddingService {
    List<Double> embedText(String text);
}
