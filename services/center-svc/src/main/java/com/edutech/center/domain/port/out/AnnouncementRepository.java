// src/main/java/com/edutech/center/domain/port/out/AnnouncementRepository.java
package com.edutech.center.domain.port.out;

import com.edutech.center.domain.model.Announcement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnnouncementRepository {
    Announcement save(Announcement announcement);
    Optional<Announcement> findById(UUID id);
    List<Announcement> findActiveByCenterId(UUID centerId);
    List<Announcement> findDue();
}
