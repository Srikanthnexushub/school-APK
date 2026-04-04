package com.edutech.center.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient aiGatewayRestClient(
            @Value("${ai-gateway.base-url}") String baseUrl,
            @Value("${service.api-key}") String serviceApiKey) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Service-Key", serviceApiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Bean
    public RestClient parentSvcRestClient(
            @Value("${parent-svc.base-url}") String baseUrl,
            @Value("${service.api-key}") String serviceApiKey) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Service-Key", serviceApiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
