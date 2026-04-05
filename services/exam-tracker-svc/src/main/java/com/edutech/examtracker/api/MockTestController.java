package com.edutech.examtracker.api;

import com.edutech.examtracker.application.dto.MockTestAttemptResponse;
import com.edutech.examtracker.application.dto.RecordMockTestRequest;
import com.edutech.examtracker.application.exception.ExamAccessDeniedException;
import com.edutech.examtracker.domain.port.in.GetMockTestHistoryUseCase;
import com.edutech.examtracker.domain.port.in.RecordMockTestUseCase;
import com.edutech.examtracker.infrastructure.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
public class MockTestController {

    private final RecordMockTestUseCase recordMockTestUseCase;
    private final GetMockTestHistoryUseCase getMockTestHistoryUseCase;

    public MockTestController(RecordMockTestUseCase recordMockTestUseCase,
                              GetMockTestHistoryUseCase getMockTestHistoryUseCase) {
        this.recordMockTestUseCase = recordMockTestUseCase;
        this.getMockTestHistoryUseCase = getMockTestHistoryUseCase;
    }

    @PostMapping("/students/{studentId}/mock-tests")
    public ResponseEntity<MockTestAttemptResponse> recordMockTest(
            @PathVariable UUID studentId,
            @Valid @RequestBody RecordMockTestRequest request) {
        AuthPrincipal principal = AuthPrincipal.current();
        validateAccess(principal, studentId);
        MockTestAttemptResponse response = recordMockTestUseCase.recordMockTest(studentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/students/{studentId}/mock-tests")
    public ResponseEntity<Page<MockTestAttemptResponse>> getMockTests(
            @PathVariable UUID studentId,
            @RequestParam(required = false) UUID enrollmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        AuthPrincipal principal = AuthPrincipal.current();
        validateAccess(principal, studentId);
        PageRequest pageRequest = PageRequest.of(page, size);
        if (enrollmentId != null) {
            List<MockTestAttemptResponse> all = getMockTestHistoryUseCase.getMockHistory(studentId, enrollmentId);
            int start = (int) pageRequest.getOffset();
            int end = Math.min(start + pageRequest.getPageSize(), all.size());
            List<MockTestAttemptResponse> content = start >= all.size() ? List.of() : all.subList(start, end);
            return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(content, pageRequest, all.size()));
        }
        List<MockTestAttemptResponse> allByStudent = getMockTestHistoryUseCase.getMockHistoryByStudent(studentId);
        int start2 = (int) pageRequest.getOffset();
        int end2 = Math.min(start2 + pageRequest.getPageSize(), allByStudent.size());
        List<MockTestAttemptResponse> content2 = start2 >= allByStudent.size() ? List.of() : allByStudent.subList(start2, end2);
        return ResponseEntity.ok(new org.springframework.data.domain.PageImpl<>(content2, pageRequest, allByStudent.size()));
    }

    // ── Access control ────────────────────────────────────────────────────────

    private void validateAccess(AuthPrincipal principal, UUID studentId) {
        if (principal.isSuperAdmin() || principal.isInstitutionAdmin()) return;
        if (principal.isCenterAdmin()) return;
        if (principal.isTeacher()) return;
        if (principal.isParent()) return;
        if (principal.isStudent() && principal.userId().equals(studentId)) return;
        throw new ExamAccessDeniedException("Access denied to mock test data for student: " + studentId);
    }
}
