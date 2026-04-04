package com.edutech.center.infrastructure.http;

import com.edutech.center.domain.port.out.ParentLinkValidationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

/**
 * Calls parent-svc GET /api/v1/internal/parents/{parentUserId}/linked-center-ids
 * (secured by X-Service-Key) to validate whether a parent has an active student
 * link at the target center.
 *
 * Fail-closed: any exception (network, timeout, 4xx, 5xx) returns false so
 * center-svc never grants PARENT access due to a connectivity issue.
 */
@Component
public class ParentSvcLinkValidationAdapter implements ParentLinkValidationPort {

    private static final Logger log = LoggerFactory.getLogger(ParentSvcLinkValidationAdapter.class);

    private final RestClient restClient;

    public ParentSvcLinkValidationAdapter(RestClient parentSvcRestClient) {
        this.restClient = parentSvcRestClient;
    }

    @Override
    public boolean isParentLinkedToCenter(UUID parentUserId, UUID centerId) {
        try {
            List<UUID> centerIds = restClient.get()
                    .uri("/api/v1/internal/parents/{parentUserId}/linked-center-ids", parentUserId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<UUID>>() {});

            if (centerIds == null || centerIds.isEmpty()) {
                return false;
            }
            return centerIds.contains(centerId);
        } catch (Exception e) {
            log.warn("Parent link validation failed for parentUserId={} centerId={}: {}",
                    parentUserId, centerId, e.getMessage());
            return false; // fail-closed
        }
    }
}
