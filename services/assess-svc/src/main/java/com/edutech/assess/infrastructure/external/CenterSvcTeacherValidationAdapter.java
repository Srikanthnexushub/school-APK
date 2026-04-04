// src/main/java/com/edutech/assess/infrastructure/external/CenterSvcTeacherValidationAdapter.java
package com.edutech.assess.infrastructure.external;

import com.edutech.assess.domain.port.out.TeacherCenterValidationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Calls center-svc to verify teacher–center membership.
 *
 * Endpoint: GET /api/v1/centers/{centerId}/teachers?page=0&size=200
 * Returns a Spring Page JSON; content[] each has a "userId" field.
 *
 * Fail-closed: any exception (timeout, 4xx, 5xx, deserialization) returns false.
 */
@Component
public class CenterSvcTeacherValidationAdapter implements TeacherCenterValidationPort {

    private static final Logger log = LoggerFactory.getLogger(CenterSvcTeacherValidationAdapter.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final int PAGE_SIZE = 200;

    private final WebClient webClient;

    public CenterSvcTeacherValidationAdapter(
            WebClient.Builder webClientBuilder,
            @Value("${center-svc.base-url}") String centerSvcBaseUrl,
            @Value("${service.api-key}") String serviceApiKey) {
        this.webClient = webClientBuilder
                .baseUrl(centerSvcBaseUrl)
                .defaultHeader("X-Service-Key", serviceApiKey)
                .build();
    }

    @Override
    public boolean isTeacherAtCenter(UUID teacherUserId, UUID centerId) {
        try {
            Map<String, Object> page = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/centers/{centerId}/teachers")
                            .queryParam("page", "0")
                            .queryParam("size", String.valueOf(PAGE_SIZE))
                            .build(centerId))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .onErrorReturn(Map.of())
                    .block(TIMEOUT);

            if (page == null || page.isEmpty()) {
                log.warn("Teacher center validation: empty response from center-svc for centerId={}", centerId);
                return false;
            }

            Object contentObj = page.get("content");
            if (!(contentObj instanceof List<?> content)) {
                log.warn("Teacher center validation: unexpected response shape from center-svc for centerId={}", centerId);
                return false;
            }

            return content.stream()
                    .filter(item -> item instanceof Map)
                    .map(item -> (Map<?, ?>) item)
                    .anyMatch(teacher -> {
                        Object userId = teacher.get("userId");
                        return userId != null && teacherUserId.toString().equals(userId.toString());
                    });

        } catch (Exception e) {
            log.warn("Teacher center validation failed: teacherUserId={} centerId={} error={}",
                    teacherUserId, centerId, e.getMessage());
            return false; // fail-closed
        }
    }
}
