package com.example.fashionshop.service.impl;

import com.example.fashionshop.dto.chat.ChatDto;
import com.example.fashionshop.service.ChatService;
import com.example.fashionshop.service.GeminiChatService;
import com.example.fashionshop.service.VectorSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
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

        QUAN TRỌNG: Dựa vào lịch sử hội thoại bên dưới để hiểu đúng ngữ cảnh.
        Nếu khách dùng đại từ như "cái đó", "sản phẩm đó", "nó", "cái này" — hãy xác định
        chính xác khách đang nói tới sản phẩm CỤ THỂ nào dựa trên tin nhắn trước đó,
        và CHỈ trả lời/tập trung vào đúng sản phẩm đó, không liệt kê lại toàn bộ danh sách.

        Danh sách sản phẩm liên quan:
        %s

        Lịch sử hội thoại trước đó:
        %s
        """;

    private static final List<String> PRODUCT_INTENT_KEYWORDS = List.of(
            "gợi ý", "goi y",
            "sản phẩm nào", "san pham nao",
            "cho tôi xem", "cho toi xem",
            "có gì", "co gi",
            "còn gì", "con gi",
            "danh sách", "danh sach",
            "xem sản phẩm", "xem san pham",
            "link", "đường dẫn",
            "sản phẩm đó", "san pham do",
            "cái đó", "cai do"
    );

    private static final double RELEVANCE_THRESHOLD = 0.5;

    @Override
    public ChatDto.Response chat(String userMessage, List<ChatDto.ChatMessage> history) {
        // Ghép 1-2 tin nhắn gần nhất vào câu tìm kiếm, giúp semantic search "hiểu" đại từ
        String searchQuery = buildSearchQuery(userMessage, history);

        List<VectorSearchService.ScoredProduct> relevant =
                vectorSearchService.findRelevantProducts(searchQuery, 5);

        String productContext = relevant.stream()
                .map(sp -> "- " + sp.sourceText())
                .collect(Collectors.joining("\n"));

        String historyContext = buildHistoryContext(history);

        String systemPrompt = SYSTEM_PROMPT.formatted(
                productContext.isBlank() ? "Không có sản phẩm nào liên quan." : productContext,
                historyContext.isBlank() ? "Chưa có." : historyContext
        );

        String reply = geminiChatService.generateReply(systemPrompt, userMessage);

        List<ChatDto.SuggestedProduct> suggestions = wantsProductList(userMessage)
                ? relevant.stream()
                .filter(sp -> sp.score() >= RELEVANCE_THRESHOLD) // chỉ lấy sản phẩm đủ liên quan
                .limit(3)
                .map(sp -> ChatDto.SuggestedProduct.builder()
                        .id(sp.product().getId())
                        .name(sp.product().getName())
                        .slug(sp.product().getSlug())
                        .build())
                .toList()
                : List.of();

        return ChatDto.Response.builder()
                .reply(reply)
                .suggestedProducts(suggestions)
                .build();
    }

    /**
     * Ghép tin nhắn hiện tại với 1-2 lượt gần nhất để câu tìm kiếm mang đủ ngữ cảnh.
     * Ví dụ: "cho tôi xem sản phẩm đó" + lịch sử có "Áo phông mùa hè"
     * -> câu tìm kiếm thực tế: "Áo phông mùa hè cho tôi xem sản phẩm đó"
     * -> semantic search sẽ ưu tiên đúng sản phẩm đó thay vì lẫn lộn.
     */
    private String buildSearchQuery(String userMessage, List<ChatDto.ChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return userMessage;
        }

        int fromIndex = Math.max(0, history.size() - 2);
        String recentContext = history.subList(fromIndex, history.size()).stream()
                .map(ChatDto.ChatMessage::getText)
                .collect(Collectors.joining(" "));

        return recentContext + " " + userMessage;
    }

    private boolean wantsProductList(String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        return PRODUCT_INTENT_KEYWORDS.stream().anyMatch(normalized::contains);
    }

    private String buildHistoryContext(List<ChatDto.ChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return "";
        }

        int fromIndex = Math.max(0, history.size() - 6);

        return history.subList(fromIndex, history.size()).stream()
                .map(m -> ("user".equals(m.getRole()) ? "Khách: " : "Bot: ") + m.getText())
                .collect(Collectors.joining("\n"));
    }
}