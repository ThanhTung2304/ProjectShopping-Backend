package com.example.fashionshop.service.impl;

import com.example.fashionshop.service.GeminiChatService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class GeminiChatServiceImpl implements GeminiChatService {

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.gemini.api-key}")
    private String apiKey;

    @Value("${app.gemini.chat-model}")
    private String chatModel;

    @Override
    public String generateReply(String systemContext, String userMessage) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + chatModel + ":generateContent?key=" + apiKey;

        String fullPrompt = systemContext + "\n\nCâu hỏi của khách hàng: " + userMessage;

        String requestBody = """
            {
              "contents": [{
                "parts": [{ "text": "%s" }]
              }],
              "generationConfig": {
                "temperature": 0.3,
                "topP": 0.9,
                "maxOutputTokens": 600
              }
            }
            """.formatted(escapeJson(fullPrompt));

        try {
            String response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            return root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText("Xin lỗi, tôi chưa thể trả lời câu hỏi này.");

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi gọi Gemini Chat API: " + e.getMessage(), e);
        }
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}