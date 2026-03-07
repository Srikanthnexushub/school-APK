package com.edutech.mentorsvc.domain.port.in;

import com.edutech.mentorsvc.application.dto.BookSessionRequest;
import com.edutech.mentorsvc.application.dto.MentorSessionResponse;

public interface BookMentorSessionUseCase {
    MentorSessionResponse bookSession(BookSessionRequest request);
}
