package com.edutech.mentorsvc.domain.port.in;

import com.edutech.mentorsvc.application.dto.MentorProfileResponse;

import java.util.List;
import java.util.UUID;

public interface GetMentorProfileUseCase {
    MentorProfileResponse getMentorById(UUID mentorId);
    MentorProfileResponse getMentorByUserId(UUID userId);
    List<MentorProfileResponse> getAllAvailableMentors();
}
