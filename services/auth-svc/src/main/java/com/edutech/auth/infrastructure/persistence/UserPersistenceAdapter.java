// src/main/java/com/edutech/auth/infrastructure/persistence/UserPersistenceAdapter.java
package com.edutech.auth.infrastructure.persistence;

import com.edutech.auth.domain.model.User;
import com.edutech.auth.domain.port.out.UserRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound adapter: implements the UserRepository port using Spring Data JPA.
 * Isolates the domain from the persistence framework.
 */
@Repository
public class UserPersistenceAdapter implements UserRepository {

    private final SpringDataUserRepository jpaRepository;

    public UserPersistenceAdapter(SpringDataUserRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public User save(User user) {
        return jpaRepository.save(user);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public List<User> findExpiredPendingVerification(Instant cutoff) {
        return jpaRepository.findExpiredPendingVerification(cutoff);
    }

    @Override
    public Optional<User> findByProviderAndProviderId(String provider, String providerId) {
        return jpaRepository.findByProviderAndProviderId(provider, providerId);
    }
}
