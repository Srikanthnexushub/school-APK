package com.edutech.aigateway.infrastructure.config;

import com.edutech.aigateway.api.VoiceWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.Map;

/**
 * Registers the voice WebSocket handler at {@code /api/v1/ai/voice/ws}.
 *
 * <p>Order -2 puts this mapping before Spring MVC's default handler mapping
 * so the WebSocket upgrade is intercepted first.</p>
 */
@Configuration
public class VoiceWebSocketConfig {

    @Bean
    public HandlerMapping voiceWebSocketHandlerMapping(VoiceWebSocketHandler handler) {
        Map<String, WebSocketHandler> urlMap = Map.of("/api/v1/ai/voice/ws", handler);
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(urlMap);
        mapping.setOrder(-2);
        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter webSocketHandlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
