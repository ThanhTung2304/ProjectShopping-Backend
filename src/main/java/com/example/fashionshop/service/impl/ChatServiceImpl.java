package com.example.fashionshop.service.impl;

import com.example.fashionshop.dto.chat.ChatDto;
import com.example.fashionshop.service.ChatService;
import com.example.fashionshop.service.GeminiChatService;
import com.example.fashionshop.service.VectorSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final VectorSearchService vectorSearchService;
    private final GeminiChatService geminiChatService;

    private static final String SYSTEM_PROMPT = """
        Bạn là trợ lý tư vấn bán hàng của shop thời trang LEANH Studio.
        Hãy trả lời thân thiện, ngắn gọn, bằng tiếng Việt.
        Chỉ tư vấn dựa trên thông tin sản phẩm được cung cấp bên dưới.
        Nếu không có sản phẩm phù hợp trong danh sách, hãy nói thật là hiện chưa có sản phẩm đó,
        đừng bịa thông tin không có trong dữ liệu.

        Danh sách sản phẩm liên quan:
        %s
        """;

    @Override
    public ChatDto.Response chat(String userMessage) {
        List<VectorSearchService.ScoredProduct> relevant =
                vectorSearchService.findRelevantProducts(userMessage, 5);

        String productContext = relevant.stream()
                .map(sp -> "- " + sp.product().getName() + ": " +
                        (sp.product().getDescription() != null ? sp.product().getDescription() : ""))
                .collect(Collectors.joining("\n"));

        String systemPrompt = SYSTEM_PROMPT.formatted(
                productContext.isBlank() ? "Không có sản phẩm nào liên quan." : productContext
        );

        String reply = geminiChatService.generateReply(systemPrompt, userMessage);

        List<ChatDto.SuggestedProduct> suggestions = relevant.stream()
                .map(sp -> ChatDto.SuggestedProduct.builder()
                        .id(sp.product().getId())
                        .name(sp.product().getName())
                        .slug(sp.product().getSlug())
                        .build())
                .toList();

        return ChatDto.Response.builder()
                .reply(reply)
                .suggestedProducts(suggestions)
                .build();
    }
}