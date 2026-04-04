// src/main/java/com/edutech/auth/infrastructure/kafka/AuditEventConsumer.java
package com.edutech.auth.infrastructure.kafka;

import com.edutech.auth.domain.model.AuditEventLog;
import com.edutech.auth.infrastructure.persistence.SpringDataAuditEventLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka consumer for the audit-immutable topic.
 *
 * Persists every audit event to auth_schema.audit_events_log, giving the platform
 * a queryable, DB-backed audit trail that survives Kafka log compaction or retention
 * expiry. Rows are never updated or deleted (append-only).
 *
 * Error handling: the companion KafkaConsumerConfig attaches a DefaultErrorHandler
 * with FixedBackOff(0, 0) — a poison-pill message is logged and skipped rather than
 * blocking the partition indefinitely.
 */
@Component
public class AuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventConsumer.class);

    private final SpringDataAuditEventLogRepository repository;
    private final ObjectMapper objectMapper;

    public AuditEventConsumer(SpringDataAuditEventLogRepository repository,
                              ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * Consumes raw JSON from audit-immutable and persists it as an AuditEventLog row.
     *
     * The topic carries heterogeneous event types (UserAuthenticatedEvent,
     * UserRegisteredEvent, etc.) serialised as JSON by the producer. We parse the
     * common fields (eventId, userId, email, ipAddress, occurredAt) and store the
     * full payload as the {@code details} JSONB column so no information is lost.
     */
    @KafkaListener(
            topics = "${kafka.topics.audit-immutable}",
            groupId = "auth-svc-audit-consumer",
            containerFactory = "auditEventKafkaListenerContainerFactory"
    )
    public void consumeAuditEvent(@Payload String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);

            String eventType = extractText(root, "@class", "eventId") != null
                    ? inferEventType(root)
                    : "UNKNOWN";

            UUID userId = extractUuid(root, "userId");
            String action = inferAction(eventType, root);
            String ipAddress = extractText(root, "ipAddress");
            Instant timestamp = extractInstant(root, "occurredAt");

            AuditEventLog entry = AuditEventLog.create(
                    eventType, userId, action, rawJson, ipAddress, timestamp
            );
            repository.save(entry);

            log.debug("Audit event persisted: type={} userId={}", eventType, userId);

        } catch (Exception e) {
            // Rethrow so the DefaultErrorHandler (FixedBackOff(0,0)) can log-and-skip
            log.error("Failed to persist audit event — will be skipped. payload={} error={}",
                    rawJson, e.getMessage(), e);
            throw new RuntimeException("Audit event processing failed", e);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private String inferEventType(JsonNode root) {
        // UserAuthenticatedEvent has "success" field; UserRegisteredEvent has "role"
        if (root.has("success")) return "UserAuthenticatedEvent";
        if (root.has("role"))    return "UserRegisteredEvent";
        return "AuditEvent";
    }

    private String inferAction(String eventType, JsonNode root) {
        return switch (eventType) {
            case "UserAuthenticatedEvent" -> {
                boolean success = root.path("success").asBoolean(false);
                yield success ? "LOGIN_SUCCESS" : "LOGIN_FAILED";
            }
            case "UserRegisteredEvent" -> "USER_REGISTERED";
            default -> "AUDIT_EVENT";
        };
    }

    private String extractText(JsonNode root, String... fieldNames) {
        for (String name : fieldNames) {
            JsonNode node = root.path(name);
            if (!node.isMissingNode() && !node.isNull()) {
                return node.asText();
            }
        }
        return null;
    }

    private UUID extractUuid(JsonNode root, String fieldName) {
        String val = extractText(root, fieldName);
        if (val == null || val.isBlank()) return null;
        try {
            return UUID.fromString(val);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Instant extractInstant(JsonNode root, String fieldName) {
        String val = extractText(root, fieldName);
        if (val == null || val.isBlank()) return Instant.now();
        try {
            return Instant.parse(val);
        } catch (Exception e) {
            return Instant.now();
        }
    }
}
