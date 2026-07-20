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
        
        CÁCH TRẢ LỜI:
            - Đọc kỹ câu hỏi của khách, xác định CHÍNH XÁC khách đang hỏi về khía cạnh nào
              (giá, size, màu sắc, chất liệu, cách phối đồ, tồn kho...) và trả lời thẳng vào đó trước tiên.
            - Sau khi trả lời đúng trọng tâm, có thể bổ sung 1-2 thông tin liên quan hữu ích khác
              (ví dụ nếu hỏi giá thì có thể nhắc thêm size/màu đang có sẵn).
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

    // Từ khóa thể hiện khách MUỐN SO SÁNH / xem nhiều lựa chọn
    private static final List<String> MULTIPLE_OPTIONS_KEYWORDS = List.of(
            "khác", "khac",
            "so sánh", "so sanh",
            "còn loại nào", "con loai nao",
            "còn mẫu nào", "con mau nao",
            "tất cả", "tat ca",
            "toàn bộ", "toan bo"
    );

    private static final double RELEVANCE_THRESHOLD = 0.5;

    @Override
    public ChatDto.Response chat(String userMessage, List<ChatDto.ChatMessage> history) {
        String searchQuery = buildSearchQuery(userMessage, history);

        List<VectorSearchService.ScoredProduct> allRelevant =
                vectorSearchService.findRelevantProducts(searchQuery, 5);

        // Lọc theo ngưỡng liên quan trước khi làm bất cứ điều gì khác
        List<VectorSearchService.ScoredProduct> aboveThreshold = allRelevant.stream()
                .filter(sp -> sp.score() >= RELEVANCE_THRESHOLD)
                .toList();

        boolean wantsMultiple = wantsMultipleOptions(userMessage);
        List<VectorSearchService.ScoredProduct> contextProducts = wantsMultiple
                ? aboveThreshold.stream().limit(3).toList()
                : aboveThreshold.stream().limit(1).toList();

        String productContext = contextProducts.stream()
                .map(sp -> "- " + sp.sourceText())
                .collect(Collectors.joining("\n"));

        String historyContext = buildHistoryContext(history);

        String systemPrompt = SYSTEM_PROMPT.formatted(
                productContext.isBlank() ? "Không có sản phẩm nào phù hợp." : productContext,
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