package com.example.fashionshop.controller;

import com.example.fashionshop.dto.chat.ChatDto;
import com.example.fashionshop.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ChatDto.Response chat(@Valid @RequestBody ChatDto.Request request) {
        return chatService.chat(request.getMessage(), request.getHistory());
    }
}