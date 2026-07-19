package com.example.fashionshop.service.impl;

import com.example.fashionshop.service.GeminiEmbeddingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
public class GeminiEmbeddingServiceImpl implements GeminiEmbeddingService {

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.gemini.api-key}")
    private String apiKey;

    private static final String EMBED_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent?key=";

    //Nhận một đoạn văn bản và trả về vector embedding.
    @Override
    public List<Double> embedText(String text) {
        String requestBody = """
    {
      "model": "models/gemini-embedding-001",
      "content": {
        "parts": [{ "text": "%s" }]
      }
    }
    """.formatted(text.replace("\"", "\\\"").replace("\n", " "));

        try{
            String respone = restClient.post()
                    .uri(EMBED_URL + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(respone);
            JsonNode valueNode = root.path("embedding").path("values");

            return valueNode.isArray()
                    ? objectMapper.convertValue(valueNode, List.class)
                    : List.of();
        }
        catch (Exception e){
            throw new RuntimeException("Lỗi khi gọi API Gemini Embedding: " + e.getMessage(), e);
        }
    }
}
