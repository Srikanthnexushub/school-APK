// src/main/java/com/edutech/auth/infrastructure/persistence/SpringDataUserRepository.java
package com.edutech.auth.infrastructure.persistence;

import com.edutech.auth.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}
