package com.edutech.mentorsvc.api;

import com.edutech.mentorsvc.application.dto.BookSessionRequest;
import com.edutech.mentorsvc.application.dto.MentorSessionResponse;
import com.edutech.mentorsvc.domain.port.in.BookMentorSessionUseCase;
import com.edutech.mentorsvc.domain.port.in.CompleteSessionUseCase;
import com.edutech.mentorsvc.domain.port.in.GetMentorSessionUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/v1/mentor-sessions")
@Tag(name = "Mentor Sessions", description = "Book and manage mentor sessions")
public class MentorSessionController {

    private final BookMentorSessionUseCase bookMentorSessionUseCase;
    private final GetMentorSessionUseCase getMentorSessionUseCase;
    private final CompleteSessionUseCase completeSessionUseCase;

    public MentorSessionController(BookMentorSessionUseCase bookMentorSessionUseCase,
                                   GetMentorSessionUseCase getMentorSessionUseCase,
                                   CompleteSessionUseCase completeSessionUseCase) {
        this.bookMentorSessionUseCase = bookMentorSessionUseCase;
        this.getMentorSessionUseCase = getMentorSessionUseCase;
        this.completeSessionUseCase = completeSessionUseCase;
    }

    @PostMapping
    @Operation(summary = "Book a new mentor session")
    public ResponseEntity<MentorSessionResponse> bookSession(
            @Valid @RequestBody BookSessionRequest request) {
        MentorSessionResponse response = bookMentorSessionUseCase.bookSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "Get session by ID")
    public ResponseEntity<MentorSessionResponse> getSession(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(getMentorSessionUseCase.getSessionById(sessionId));
    }

    @GetMapping
    @Operation(summary = "List sessions by mentor or student")
    public ResponseEntity<List<MentorSessionResponse>> getSessions(
            @RequestParam(required = false) UUID mentorId,
            @RequestParam(required = false) UUID studentId) {
        if (mentorId != null) {
            return ResponseEntity.ok(getMentorSessionUseCase.getSessionsByMentor(mentorId));
        }
        if (studentId != null) {
            return ResponseEntity.ok(getMentorSessionUseCase.getSessionsByStudent(studentId));
        }
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/{sessionId}/complete")
    @Operation(summary = "Mark a session as completed")
    public ResponseEntity<MentorSessionResponse> completeSession(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(completeSessionUseCase.completeSession(sessionId));
    }
}
