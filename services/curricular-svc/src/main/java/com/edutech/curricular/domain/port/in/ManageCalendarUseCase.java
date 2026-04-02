package com.edutech.curricular.domain.port.in;

import com.edutech.curricular.application.dto.AcademicYearRequest;
import com.edutech.curricular.domain.model.AcademicYear;

import java.util.List;
import java.util.UUID;

public interface ManageCalendarUseCase {
    AcademicYear createAcademicYear(UUID centerId, AcademicYearRequest request);
    List<AcademicYear> getActiveAcademicYears(UUID centerId);
}
