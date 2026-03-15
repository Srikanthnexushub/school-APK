package com.edutech.mentorsvc.domain.port.in;

import com.edutech.mentorsvc.application.dto.MentorProfileResponse;
import com.edutech.mentorsvc.application.dto.UpdateMentorProfileRequest;

import java.util.UUID;

public interface UpdateMentorProfileUseCase {
    MentorProfileResponse updateProfile(UUID userId, UpdateMentorProfileRequest request);
}
