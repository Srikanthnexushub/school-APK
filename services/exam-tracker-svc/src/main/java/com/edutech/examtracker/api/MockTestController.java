package com.edutech.examtracker.api;

import com.edutech.examtracker.application.dto.MockTestAttemptResponse;
import com.edutech.examtracker.application.dto.RecordMockTestRequest;
import com.edutech.examtracker.domain.port.in.GetMockTestHistoryUseCase;
import com.edutech.examtracker.domain.port.in.RecordMockTestUseCase;
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
        MockTestAttemptResponse response = recordMockTestUseCase.recordMockTest(studentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/students/{studentId}/mock-tests")
    public ResponseEntity<List<MockTestAttemptResponse>> getMockTests(
            @PathVariable UUID studentId,
            @RequestParam(required = false) UUID enrollmentId) {
        List<MockTestAttemptResponse> history = enrollmentId != null
                ? getMockTestHistoryUseCase.getMockHistory(studentId, enrollmentId)
                : getMockTestHistoryUseCase.getMockHistoryByStudent(studentId);
        return ResponseEntity.ok(history);
    }
}
