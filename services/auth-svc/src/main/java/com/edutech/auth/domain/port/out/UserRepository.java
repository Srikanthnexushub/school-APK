// src/main/java/com/edutech/auth/domain/port/out/UserRepository.java
package com.edutech.auth.domain.port.out;

import com.edutech.auth.domain.model.User;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);
    Optional<User> findByEmail(String email);
    Optional<User> findById(UUID id);
    boolean existsByEmail(String email);
    List<User> findExpiredPendingVerification(Instant cutoff);
}
