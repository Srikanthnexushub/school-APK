package com.edutech.examtracker.api;

import com.edutech.examtracker.application.dto.RecordStudySessionRequest;
import com.edutech.examtracker.application.dto.StudySessionResponse;
import com.edutech.examtracker.domain.port.in.GetStudySessionsUseCase;
import com.edutech.examtracker.domain.port.in.RecordStudySessionUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exam-tracker")
public class StudySessionController {

    private final RecordStudySessionUseCase recordStudySessionUseCase;
    private final GetStudySessionsUseCase getStudySessionsUseCase;

    public StudySessionController(RecordStudySessionUseCase recordStudySessionUseCase,
                                  GetStudySessionsUseCase getStudySessionsUseCase) {
        this.recordStudySessionUseCase = recordStudySessionUseCase;
        this.getStudySessionsUseCase = getStudySessionsUseCase;
    }

    @PostMapping("/students/{studentId}/study-sessions")
    public ResponseEntity<StudySessionResponse> recordSession(
            @PathVariable UUID studentId,
            @Valid @RequestBody RecordStudySessionRequest request) {
        StudySessionResponse response = recordStudySessionUseCase.recordSession(studentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/students/{studentId}/study-sessions")
    public ResponseEntity<List<StudySessionResponse>> getStudySessions(
            @PathVariable UUID studentId,
            @RequestParam UUID enrollmentId) {
        List<StudySessionResponse> sessions = getStudySessionsUseCase.getStudySessions(studentId, enrollmentId);
        return ResponseEntity.ok(sessions);
    }
}
