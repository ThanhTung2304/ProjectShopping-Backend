package com.example.fashionshop.dto.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.util.List;

public class ChatDto {

    @Getter
    @Setter
    public static class Request {
        @NotBlank(message = "Nội dung tin nhắn không được để trống")
        private String message;

        private  List<ChatMessage> history;
    }

    @Getter
    @Setter
    public static class ChatMessage {
        private String role; // "user" hoặc "assistant"
        private String text;
    }

    @Getter
    @Builder
    public static class Response {
        private String reply;
        private List<SuggestedProduct> suggestedProducts;
    }

    @Getter
    @Builder
    public static class SuggestedProduct {
        private Long id;
        private String name;
        private String slug;
    }
}