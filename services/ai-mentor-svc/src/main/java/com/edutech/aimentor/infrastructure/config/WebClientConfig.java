package com.edutech.aimentor.infrastructure.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(KafkaTopicProperties.class)
public class WebClientConfig {

    private final String aiGatewayBaseUri;
    private final int timeoutSeconds;
    private final String serviceApiKey;

    public WebClientConfig(
            @Value("${ai-gateway.base-uri}") String aiGatewayBaseUri,
            @Value("${ai-gateway.timeout-seconds}") int timeoutSeconds,
            @Value("${service.api-key}") String serviceApiKey) {
        this.aiGatewayBaseUri = aiGatewayBaseUri;
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
                .baseUrl(aiGatewayBaseUri)
                .defaultHeader("X-Service-Key", serviceApiKey)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
