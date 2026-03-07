package com.edutech.careeroracle.domain.port.in;

import com.edutech.careeroracle.application.dto.CareerProfileResponse;
import com.edutech.careeroracle.application.dto.CreateCareerProfileRequest;

public interface CreateCareerProfileUseCase {

    CareerProfileResponse createCareerProfile(CreateCareerProfileRequest request);
}
