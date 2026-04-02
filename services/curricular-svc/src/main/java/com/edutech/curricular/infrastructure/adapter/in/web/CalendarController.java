package com.edutech.curricular.infrastructure.adapter.in.web;

import com.edutech.curricular.application.dto.AcademicYearRequest;
import com.edutech.curricular.application.dto.AcademicYearResponse;
import com.edutech.curricular.domain.model.AcademicYear;
import com.edutech.curricular.domain.port.in.ManageCalendarUseCase;
import com.edutech.curricular.infrastructure.config.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/curricular")
@RequiredArgsConstructor
public class CalendarController {

    private final ManageCalendarUseCase calendarUseCase;

    @GetMapping("/center/{centerId}/calendar")
    public ResponseEntity<List<AcademicYearResponse>> getCalendar(
            @PathVariable UUID centerId,
            @AuthenticationPrincipal AuthPrincipal principal) {
        List<AcademicYearResponse> years = calendarUseCase.getActiveAcademicYears(centerId).stream()
            .map(ay -> new AcademicYearResponse(ay.getId(), ay.getCenterId(), ay.getYearLabel(), ay.getStartDate(), ay.getEndDate(), ay.isActive()))
            .toList();
        return ResponseEntity.ok(years);
    }

    @PostMapping("/center/{centerId}/calendar")
    public ResponseEntity<?> createAcademicYear(
            @PathVariable UUID centerId,
            @RequestBody AcademicYearRequest request,
            @AuthenticationPrincipal AuthPrincipal principal) {
        if (!"CENTER_ADMIN".equals(principal.role())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        AcademicYear ay = calendarUseCase.createAcademicYear(centerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new AcademicYearResponse(ay.getId(), ay.getCenterId(), ay.getYearLabel(), ay.getStartDate(), ay.getEndDate(), ay.isActive()));
    }
}
