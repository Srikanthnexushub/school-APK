package com.edutech.mentorsvc.domain.port.in;

import com.edutech.mentorsvc.application.dto.MentorProfileResponse;
import com.edutech.mentorsvc.application.dto.RegisterMentorRequest;

public interface RegisterMentorUseCase {
    MentorProfileResponse registerMentor(RegisterMentorRequest request);
}
