// src/main/java/com/edutech/auth/domain/port/out/AuditEventPublisher.java
package com.edutech.auth.domain.port.out;

/**
 * Outbound port for publishing immutable audit events.
 * Infrastructure adapter publishes to the append-only Kafka audit topic.
 */
public interface AuditEventPublisher {
    void publish(Object event);
}
