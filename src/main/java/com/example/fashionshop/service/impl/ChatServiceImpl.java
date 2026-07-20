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
        Hãy trả lời thân thiện, bằng tiếng Việt.

        CÁCH TRẢ LỜI:
        - Đọc kỹ câu hỏi của khách, xác định CHÍNH XÁC khách đang hỏi về khía cạnh nào
          (giá, size, màu sắc, chất liệu, cách phối đồ, tồn kho...) và trả lời thẳng vào đó trước tiên.
        - Nếu khách chỉ chào hỏi xã giao (VD: "xin chào", "hi", "chào shop") mà CHƯA hỏi gì cụ thể
          về sản phẩm, hãy chào lại thân thiện và hỏi khách cần tìm loại sản phẩm gì,
          KHÔNG tự ý giới thiệu/liệt kê sản phẩm khi chưa được hỏi.
        - Nếu câu hỏi mơ hồ hoặc thiếu thông tin để trả lời chính xác, hãy hỏi lại 1 câu làm rõ
          thay vì đoán bừa hoặc liệt kê chung chung.
        - Câu trả lời nên đủ chi tiết (không quá ngắn cụt lủn), nhưng không lan man dài dòng.

        QUY TẮC BẮT BUỘC:
        - CHỈ trả lời dựa trên thông tin sản phẩm được cung cấp bên dưới. Không bịa thông tin.
        - CHỈ trả lời về (các) sản phẩm được liệt kê bên dưới, đây đã là sản phẩm phù hợp nhất
          với câu hỏi hiện tại. KHÔNG tự ý giới thiệu thêm sản phẩm khác ngoài danh sách này,
          trừ khi khách rõ ràng hỏi "còn gì khác", "sản phẩm khác", "so sánh"...
        - Nếu không có sản phẩm phù hợp trong danh sách, nói thật là hiện chưa có, đừng bịa.
        - Dựa vào lịch sử hội thoại để hiểu ngữ cảnh, không lặp lại y hệt câu đã nói trước đó.

        Sản phẩm phù hợp với câu hỏi hiện tại:
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

    private static final List<String> MULTIPLE_OPTIONS_KEYWORDS = List.of(
            "khác", "khac",
            "so sánh", "so sanh",
            "còn loại nào", "con loai nao",
            "còn mẫu nào", "con mau nao",
            "tất cả", "tat ca",
            "toàn bộ", "toan bo"
    );

    // Câu chào/hỏi han xã giao thuần túy, KHÔNG mang ý định hỏi về sản phẩm
    private static final List<String> GREETING_ONLY_PATTERNS = List.of(
            "xin chào", "xin chao",
            "chào bạn", "chao ban",
            "chào shop", "chao shop",
            "hi", "hello", "alo",
            "shop ơi", "shop oi",
            "cho hỏi", "cho hoi"
    );

    private static final double RELEVANCE_THRESHOLD = 0.5;

    @Override
    public ChatDto.Response chat(String userMessage, List<ChatDto.ChatMessage> history) {
        boolean isGreetingOnly = isGreetingOnly(userMessage, history);

        List<VectorSearchService.ScoredProduct> contextProducts = List.of();

        if (!isGreetingOnly) {
            String searchQuery = buildSearchQuery(userMessage, history);

            List<VectorSearchService.ScoredProduct> allRelevant =
                    vectorSearchService.findRelevantProducts(searchQuery, 5);

            List<VectorSearchService.ScoredProduct> aboveThreshold = allRelevant.stream()
                    .filter(sp -> sp.score() >= RELEVANCE_THRESHOLD)
                    .toList();

            boolean wantsMultiple = wantsMultipleOptions(userMessage);
            contextProducts = wantsMultiple
                    ? aboveThreshold.stream().limit(3).toList()
                    : aboveThreshold.stream().limit(1).toList();
        }

        String productContext = contextProducts.stream()
                .map(sp -> "- " + sp.sourceText())
                .collect(Collectors.joining("\n"));

        String historyContext = buildHistoryContext(history);

        String systemPrompt = SYSTEM_PROMPT.formatted(
                productContext.isBlank() ? "Chưa xác định, khách chưa hỏi cụ thể về sản phẩm nào." : productContext,
                historyContext.isBlank() ? "Chưa có." : historyContext
        );

        String reply = geminiChatService.generateReply(systemPrompt, userMessage);

        List<ChatDto.SuggestedProduct> suggestions = wantsProductList(userMessage)
                ? contextProducts.stream()
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
     * Câu hỏi được coi là "chỉ chào hỏi" khi:
     * - Đây là tin nhắn ĐẦU TIÊN của cuộc trò chuyện (chưa có lịch sử), VÀ
     * - Nội dung khớp với các mẫu chào hỏi xã giao thuần túy, VÀ
     * - Không chứa từ khóa thể hiện ý định hỏi sản phẩm
     */
    private boolean isGreetingOnly(String message, List<ChatDto.ChatMessage> history) {
        boolean isFirstMessage = history == null || history.isEmpty();
        if (!isFirstMessage) {
            return false; // đã có lịch sử -> không coi là chào hỏi đơn thuần nữa
        }

        String normalized = message.toLowerCase(Locale.ROOT).trim();

        boolean matchesGreeting = GREETING_ONLY_PATTERNS.stream().anyMatch(normalized::contains);
        boolean hasProductIntent = PRODUCT_INTENT_KEYWORDS.stream().anyMatch(normalized::contains);

        // Câu ngắn (dưới 15 ký tự) và khớp mẫu chào -> chắc chắn chỉ là chào hỏi
        boolean isShortAndGreeting = normalized.length() <= 15 && matchesGreeting;

        return (matchesGreeting || isShortAndGreeting) && !hasProductIntent;
    }

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

    private boolean wantsMultipleOptions(String message) {
        String normalized = message.toLowerCase(Locale.ROOT);
        return MULTIPLE_OPTIONS_KEYWORDS.stream().anyMatch(normalized::contains);
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