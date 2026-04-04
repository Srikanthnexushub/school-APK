package com.edutech.psych.infrastructure.external;

import com.edutech.psych.domain.port.out.ParentStudentLinkValidationPort;
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
 * Calls parent-svc to validate a parent–student link.
 *
 * Endpoint: GET /api/v1/parents/by-student?studentId={studentId}
 * Returns a list of parent profile objects; each has a "userId" field.
 *
 * Fail-closed: any exception (timeout, 4xx, 5xx, deserialization) returns false.
 */
@Component
public class ParentSvcWebClientAdapter implements ParentStudentLinkValidationPort {

    private static final Logger log = LoggerFactory.getLogger(ParentSvcWebClientAdapter.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;

    public ParentSvcWebClientAdapter(
            WebClient.Builder webClientBuilder,
            @Value("${parent-svc.base-url}") String parentSvcBaseUrl,
            @Value("${service.api-key}") String serviceApiKey) {
        this.webClient = webClientBuilder
                .baseUrl(parentSvcBaseUrl)
                .defaultHeader("X-Service-Key", serviceApiKey)
                .build();
    }

    @Override
    public boolean isParentLinkedToStudent(UUID parentUserId, UUID studentId) {
        try {
            List<Map<String, Object>> profiles = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/parents/by-student")
                            .queryParam("studentId", studentId.toString())
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .onErrorReturn(List.of())
                    .block(TIMEOUT);

            if (profiles == null || profiles.isEmpty()) {
                return false;
            }

            return profiles.stream()
                    .anyMatch(profile -> {
                        Object userId = profile.get("userId");
                        return userId != null && parentUserId.toString().equals(userId.toString());
                    });
        } catch (Exception e) {
            log.warn("Parent-student link validation failed for parentUserId={} studentId={}: {}",
                    parentUserId, studentId, e.getMessage());
            return false; // fail-closed
        }
    }
}
