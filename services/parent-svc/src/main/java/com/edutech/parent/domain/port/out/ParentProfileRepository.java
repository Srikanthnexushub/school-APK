// src/main/java/com/edutech/parent/domain/port/out/ParentProfileRepository.java
package com.edutech.parent.domain.port.out;

import com.edutech.parent.domain.model.ParentProfile;

import java.util.Optional;
import java.util.UUID;

public interface ParentProfileRepository {
    ParentProfile save(ParentProfile profile);
    Optional<ParentProfile> findById(UUID id);
    Optional<ParentProfile> findByUserId(UUID userId);
    Optional<ParentProfile> findByEmail(String email);
    boolean existsByUserId(UUID userId);
}
