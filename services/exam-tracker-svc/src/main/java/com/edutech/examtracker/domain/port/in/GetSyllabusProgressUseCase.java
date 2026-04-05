package com.edutech.examtracker.domain.port.in;

import com.edutech.examtracker.application.dto.SyllabusProgressResponse;

import java.util.UUID;

public interface GetSyllabusProgressUseCase {

    SyllabusProgressResponse getSyllabusProgress(UUID studentId, UUID enrollmentId);

    /**
     * Returns the studentId that owns the given syllabus module.
     * Used by the API layer to validate ownership before allowing an update.
     */
    UUID resolveModuleStudentId(UUID moduleId);
}
