package com.edutech.parent.infrastructure.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * WebClient configuration for outbound HTTP calls from parent-svc.
 * Used by CopilotService to call ai-gateway-svc for LLM completions.
 */
@Configuration
public class WebClientConfig {

    private final String aiGatewayBaseUrl;
    private final int timeoutSeconds;
    private final String serviceApiKey;

    public WebClientConfig(
            @Value("${ai-gateway.base-url}") String aiGatewayBaseUrl,
            @Value("${ai-gateway.timeout-seconds:30}") int timeoutSeconds,
            @Value("${service.api-key}") String serviceApiKey) {
        this.aiGatewayBaseUrl = aiGatewayBaseUrl;
        this.timeoutSeconds = timeoutSeconds;
        this.serviceApiKey = serviceApiKey;
    }

    @Bean
    public WebClient aiGatewayWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) Duration.ofSeconds(timeoutSeconds).toMillis())
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(timeoutSeconds, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(timeoutSeconds, TimeUnit.SECONDS)));

        return WebClient.builder()
                .baseUrl(aiGatewayBaseUrl)
                .defaultHeader("X-Service-Key", serviceApiKey)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
