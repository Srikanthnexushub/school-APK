package com.edutech.careeroracle.domain.port.in;

import com.edutech.careeroracle.application.dto.CareerProfileResponse;
import com.edutech.careeroracle.application.dto.UpdateCareerProfileRequest;

import java.util.UUID;

public interface UpdateCareerProfileUseCase {

    CareerProfileResponse updateCareerProfile(UUID profileId, UpdateCareerProfileRequest request);
}
