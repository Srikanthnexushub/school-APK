// src/main/java/com/edutech/auth/infrastructure/persistence/SpringDataAuditEventLogRepository.java
package com.edutech.auth.infrastructure.persistence;

import com.edutech.auth.domain.model.AuditEventLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data JPA repository for the immutable audit events log.
 * Infrastructure detail — not exposed to the application layer directly.
 */
public interface SpringDataAuditEventLogRepository extends JpaRepository<AuditEventLog, UUID> {
}
