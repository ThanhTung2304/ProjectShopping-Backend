package com.example.fashionshop.service;

import com.example.fashionshop.dto.chat.ChatDto;

import java.util.List;

public interface ChatService {
    ChatDto.Response chat(String userMessage, List<ChatDto.ChatMessage> history);
}
