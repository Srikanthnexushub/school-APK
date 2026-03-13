// src/main/java/com/edutech/auth/infrastructure/persistence/SpringDataUserRepository.java
package com.edutech.auth.infrastructure.persistence;

import com.edutech.auth.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository — infrastructure detail.
 * Not exposed to the application layer; accessed only via UserPersistenceAdapter.
 */
interface SpringDataUserRepository extends JpaRepository<User, UUID> {

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findByEmail(String email);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
           "FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.status = com.edutech.auth.domain.model.UserStatus.PENDING_VERIFICATION AND u.createdAt < :cutoff AND u.deletedAt IS NULL")
    List<User> findExpiredPendingVerification(@Param("cutoff") Instant cutoff);

    @Query("SELECT u FROM User u WHERE u.provider = :provider AND u.providerId = :providerId AND u.deletedAt IS NULL")
    Optional<User> findByProviderAndProviderId(@Param("provider") String provider,
                                               @Param("providerId") String providerId);
}
