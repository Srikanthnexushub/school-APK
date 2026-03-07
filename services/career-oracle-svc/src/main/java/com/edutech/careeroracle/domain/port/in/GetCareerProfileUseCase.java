package com.edutech.careeroracle.domain.port.in;

import com.edutech.careeroracle.application.dto.CareerProfileResponse;

import java.util.UUID;

public interface GetCareerProfileUseCase {

    CareerProfileResponse getCareerProfileByStudentId(UUID studentId);

    CareerProfileResponse getCareerProfileById(UUID profileId);
}
