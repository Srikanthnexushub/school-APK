package com.edutech.mentorsvc.infrastructure.persistence;

import com.edutech.mentorsvc.domain.model.MentorProfile;
import com.edutech.mentorsvc.domain.port.out.MentorProfileRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class MentorProfilePersistenceAdapter implements MentorProfileRepository {

    private final SpringDataMentorProfileRepository springDataRepository;

    public MentorProfilePersistenceAdapter(SpringDataMentorProfileRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public MentorProfile save(MentorProfile mentorProfile) {
        return springDataRepository.save(mentorProfile);
    }

    @Override
    public Optional<MentorProfile> findById(UUID id) {
        return springDataRepository.findById(id);
    }

    @Override
    public Optional<MentorProfile> findByUserId(UUID userId) {
        return springDataRepository.findByUserId(userId);
    }

    @Override
    public Optional<MentorProfile> findByEmail(String email) {
        return springDataRepository.findByEmail(email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return springDataRepository.existsByEmail(email);
    }

    @Override
    public List<MentorProfile> findAllAvailable() {
        return springDataRepository.findAllAvailableAndNotDeleted();
    }

    @Override
    public List<MentorProfile> findAll() {
        return springDataRepository.findAll();
    }
}
