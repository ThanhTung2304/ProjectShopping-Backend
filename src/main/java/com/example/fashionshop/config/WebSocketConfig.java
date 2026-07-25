package com.example.fashionshop.config;

import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Kênh server gửi tới client, prefix "/topic" (broadcast) và "/user" (gửi riêng 1 người)
        registry.enableSimpleBroker("/topic", "/queue");
        // Prefix client dùng khi gửi lên server (không dùng trong tính năng này nhưng khai báo cho đầy đủ)
        registry.setApplicationDestinationPrefixes("/app");
        // Prefix cho tin nhắn gửi RIÊNG tới 1 user cụ thể
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                        "http://localhost:5173",
                        "http://127.0.0.1:5173",
                        "https://project-shopping-frontend*.vercel.app"
                )
                .withSockJS();
    }
}
