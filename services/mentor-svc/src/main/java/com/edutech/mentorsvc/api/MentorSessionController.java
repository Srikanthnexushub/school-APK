package com.edutech.mentorsvc.api;

import com.edutech.mentorsvc.application.dto.BookSessionRequest;
import com.edutech.mentorsvc.application.dto.MentorSessionResponse;
import com.edutech.mentorsvc.application.dto.UpdateSessionStatusRequest;
import com.edutech.mentorsvc.domain.model.SessionStatus;
import com.edutech.mentorsvc.domain.port.in.BookMentorSessionUseCase;
import com.edutech.mentorsvc.domain.port.in.CompleteSessionUseCase;
import com.edutech.mentorsvc.domain.port.in.GetMentorSessionUseCase;
import com.edutech.mentorsvc.domain.port.in.UpdateSessionStatusUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
    private final UpdateSessionStatusUseCase updateSessionStatusUseCase;

    public MentorSessionController(BookMentorSessionUseCase bookMentorSessionUseCase,
                                   GetMentorSessionUseCase getMentorSessionUseCase,
                                   CompleteSessionUseCase completeSessionUseCase,
                                   UpdateSessionStatusUseCase updateSessionStatusUseCase) {
        this.bookMentorSessionUseCase = bookMentorSessionUseCase;
        this.getMentorSessionUseCase = getMentorSessionUseCase;
        this.completeSessionUseCase = completeSessionUseCase;
        this.updateSessionStatusUseCase = updateSessionStatusUseCase;
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
    public ResponseEntity<Page<MentorSessionResponse>> getSessions(
            @RequestParam(required = false) UUID mentorId,
            @RequestParam(required = false) UUID studentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        List<MentorSessionResponse> all;
        if (mentorId != null) {
            all = getMentorSessionUseCase.getSessionsByMentor(mentorId);
        } else if (studentId != null) {
            all = getMentorSessionUseCase.getSessionsByStudent(studentId);
        } else {
            return ResponseEntity.ok(Page.empty(pageRequest));
        }
        int start = (int) pageRequest.getOffset();
        int end = Math.min(start + pageRequest.getPageSize(), all.size());
        return ResponseEntity.ok(new PageImpl<>(start < all.size() ? all.subList(start, end) : List.of(), pageRequest, all.size()));
    }

    @PostMapping("/{sessionId}/complete")
    @Operation(summary = "Mark a session as completed")
    public ResponseEntity<MentorSessionResponse> completeSession(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(completeSessionUseCase.completeSession(sessionId));
    }

    @PatchMapping("/{sessionId}/status")
    @Operation(summary = "Update session status (IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW)")
    public ResponseEntity<MentorSessionResponse> updateStatus(
            @PathVariable UUID sessionId,
            @Valid @RequestBody UpdateSessionStatusRequest request) {
        SessionStatus status = SessionStatus.valueOf(request.status());
        return ResponseEntity.ok(updateSessionStatusUseCase.updateStatus(sessionId, status));
    }
}
