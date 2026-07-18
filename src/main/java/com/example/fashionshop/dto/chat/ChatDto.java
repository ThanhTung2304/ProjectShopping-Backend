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