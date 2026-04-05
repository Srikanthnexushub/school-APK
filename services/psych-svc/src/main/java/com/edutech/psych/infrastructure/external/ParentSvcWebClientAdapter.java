package com.edutech.psych.infrastructure.external;

import com.edutech.psych.domain.port.out.ParentStudentLinkValidationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.UUID;

/**
 * Calls parent-svc to validate a parent–student link.
 *
 * Endpoint: GET /api/v1/internal/parents/{parentUserId}/is-linked-to-student/{studentId}
 * Returns a boolean directly.
 *
 * ⛔ DO NOT call GET /api/v1/parents/by-student for this purpose.
 * That endpoint is designed for an authenticated STUDENT to discover their own parents.
 * It ignores query params and reads from the security principal — when called with
 * X-Service-Key the principal is a synthetic service account (00000000-...) which has
 * no links, so it always returns an empty list and link validation silently fails closed,
 * locking every parent out of their child's data.
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
            Boolean linked = webClient.get()
                    .uri("/api/v1/internal/parents/{parentUserId}/is-linked-to-student/{studentId}",
                            parentUserId, studentId)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .onErrorReturn(false)
                    .block(TIMEOUT);
            return Boolean.TRUE.equals(linked);
        } catch (Exception e) {
            log.warn("Parent-student link validation failed for parentUserId={} studentId={}: {}",
                    parentUserId, studentId, e.getMessage());
            return false; // fail-closed
        }
    }
}
