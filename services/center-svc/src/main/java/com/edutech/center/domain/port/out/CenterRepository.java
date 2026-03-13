// src/main/java/com/edutech/center/domain/port/out/CenterRepository.java
package com.edutech.center.domain.port.out;

import com.edutech.center.domain.model.CoachingCenter;
import com.edutech.center.domain.model.CenterStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CenterRepository {
    CoachingCenter save(CoachingCenter center);
    Optional<CoachingCenter> findById(UUID id);
    List<CoachingCenter> findAll();
    List<CoachingCenter> findByOwnerId(UUID ownerId);
    boolean existsByCode(String code);
    Optional<CoachingCenter> findByCode(String code);
    List<CoachingCenter> findByStatus(CenterStatus status);
    boolean existsByNameAndCity(String name, String city);
    long countByCodePrefix(String prefix);
    Optional<CoachingCenter> findByAdminUserId(UUID adminUserId);
}
