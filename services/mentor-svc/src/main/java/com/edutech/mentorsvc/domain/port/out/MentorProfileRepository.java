package com.edutech.mentorsvc.domain.port.out;

import com.edutech.mentorsvc.domain.model.MentorProfile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MentorProfileRepository {
    MentorProfile save(MentorProfile mentorProfile);
    Optional<MentorProfile> findById(UUID id);
    Optional<MentorProfile> findByUserId(UUID userId);
    Optional<MentorProfile> findByEmail(String email);
    boolean existsByEmail(String email);
    List<MentorProfile> findAllAvailable();
    List<MentorProfile> findAll();
}
