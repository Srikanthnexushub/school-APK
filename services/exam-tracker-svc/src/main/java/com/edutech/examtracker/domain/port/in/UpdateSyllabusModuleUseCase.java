package com.edutech.examtracker.domain.port.in;

import com.edutech.examtracker.application.dto.SyllabusModuleResponse;
import com.edutech.examtracker.application.dto.UpdateSyllabusModuleRequest;

import java.util.UUID;

public interface UpdateSyllabusModuleUseCase {

    SyllabusModuleResponse updateModule(UUID moduleId, UpdateSyllabusModuleRequest request);
}
