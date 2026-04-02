package com.edutech.curricular.application.service;

import com.edutech.curricular.application.dto.AcademicYearRequest;
import com.edutech.curricular.domain.model.AcademicYear;
import com.edutech.curricular.domain.port.in.ManageCalendarUseCase;
import com.edutech.curricular.domain.port.out.AcademicYearRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CalendarService implements ManageCalendarUseCase {

    private final AcademicYearRepository academicYearRepository;

    @Override
    @Transactional
    public AcademicYear createAcademicYear(UUID centerId, AcademicYearRequest request) {
        AcademicYear ay = AcademicYear.create(
            centerId,
            request.boardId(),
            request.gradeId(),
            request.yearLabel(),
            request.startDate(),
            request.endDate()
        );
        return academicYearRepository.save(ay);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AcademicYear> getActiveAcademicYears(UUID centerId) {
        return academicYearRepository.findByCenterIdAndActive(centerId, true);
    }
}
