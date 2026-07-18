package com.example.fashionshop.service;

import com.example.fashionshop.dto.chat.ChatDto;

public interface ChatService {
    ChatDto.Response chat(String userMessage);
}
