package com.edutech.mentorsvc.domain.port.in;

import java.util.UUID;

public interface UpdateMentorAvailabilityUseCase {
    void updateAvailability(UUID mentorId, boolean isAvailable);
}
