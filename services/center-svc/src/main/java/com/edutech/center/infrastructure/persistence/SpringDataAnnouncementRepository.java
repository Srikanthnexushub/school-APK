// src/main/java/com/edutech/center/infrastructure/persistence/SpringDataAnnouncementRepository.java
package com.edutech.center.infrastructure.persistence;

import com.edutech.center.domain.model.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface SpringDataAnnouncementRepository extends JpaRepository<Announcement, UUID> {

    @Query("SELECT a FROM Announcement a WHERE a.centerId = :centerId " +
           "AND (a.expiresAt IS NULL OR a.expiresAt > :now) " +
           "ORDER BY a.createdAt DESC")
    List<Announcement> findActiveByCenterId(@Param("centerId") UUID centerId,
                                            @Param("now") Instant now);

    @Query("SELECT a FROM Announcement a WHERE a.sentAt IS NULL " +
           "AND (a.scheduledAt IS NULL OR a.scheduledAt <= :now)")
    List<Announcement> findDue(@Param("now") Instant now);
}
