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
        Dựa vào lịch sử hội thoại bên dưới (nếu có) để trả lời đúng mạch, không lặp lại y hệt
        những gì đã nói trước đó, và không hỏi lại thông tin khách đã cung cấp rồi.

        Danh sách sản phẩm liên quan:
        %s

        Lịch sử hội thoại trước đó:
        %s
        """;

    @Override
    public ChatDto.Response chat(String userMessage, List<ChatDto.ChatMessage> history) {
        List<VectorSearchService.ScoredProduct> relevant =
                vectorSearchService.findRelevantProducts(userMessage, 5);

        String productContext = relevant.stream()
                .map(sp -> "- " + sp.sourceText())
                .collect(Collectors.joining("\n"));

        String historyContext = buildHistoryContext(history);

        String systemPrompt = SYSTEM_PROMPT.formatted(
                productContext.isBlank() ? "Không có sản phẩm nào liên quan." : productContext,
                historyContext.isBlank() ? "Chưa có." : historyContext
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

    private String buildHistoryContext(List<ChatDto.ChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return "";
        }

        // chỉ lấy tối đa 6 lượt gần nhất, tránh prompt quá dài
        int fromIndex = Math.max(0, history.size() - 6);

        return history.subList(fromIndex, history.size()).stream()
                .map(m -> ("user".equals(m.getRole()) ? "Khách: " : "Bot: ") + m.getText())
                .collect(Collectors.joining("\n"));
    }
}