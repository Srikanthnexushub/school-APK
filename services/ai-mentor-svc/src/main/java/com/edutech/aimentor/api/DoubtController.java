package com.edutech.aimentor.api;

import com.edutech.aimentor.application.dto.DoubtTicketResponse;
import com.edutech.aimentor.application.dto.SubmitDoubtRequest;
import com.edutech.aimentor.domain.port.in.GetDoubtUseCase;
import com.edutech.aimentor.domain.port.in.ListDoubtsUseCase;
import com.edutech.aimentor.domain.port.in.SubmitDoubtUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/doubts")
@Tag(name = "Doubts", description = "AI-powered doubt resolution for students")
public class DoubtController {

    private final SubmitDoubtUseCase submitDoubtUseCase;
    private final GetDoubtUseCase getDoubtUseCase;
    private final ListDoubtsUseCase listDoubtsUseCase;

    public DoubtController(SubmitDoubtUseCase submitDoubtUseCase,
                           GetDoubtUseCase getDoubtUseCase,
                           ListDoubtsUseCase listDoubtsUseCase) {
        this.submitDoubtUseCase = submitDoubtUseCase;
        this.getDoubtUseCase = getDoubtUseCase;
        this.listDoubtsUseCase = listDoubtsUseCase;
    }

    @GetMapping
    @Operation(summary = "List all doubt tickets for the authenticated student")
    public ResponseEntity<Page<DoubtTicketResponse>> listDoubts(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String userRole,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        List<DoubtTicketResponse> all = listDoubtsUseCase.listDoubts(userId);
        int start = (int) pageRequest.getOffset();
        int end = Math.min(start + pageRequest.getPageSize(), all.size());
        Page<DoubtTicketResponse> response = new org.springframework.data.domain.PageImpl<>(
                start < all.size() ? all.subList(start, end) : List.of(), pageRequest, all.size());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit a doubt question for AI resolution")
    public ResponseEntity<DoubtTicketResponse> submitDoubt(
            @Valid @RequestBody SubmitDoubtRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String userRole) {
        SubmitDoubtRequest secured = new SubmitDoubtRequest(
                userId,
                request.enrollmentId(),
                request.subjectArea(),
                request.question()
        );
        DoubtTicketResponse response = submitDoubtUseCase.submitDoubt(secured);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{doubtTicketId}")
    @Operation(summary = "Get a doubt ticket by ID")
    public ResponseEntity<DoubtTicketResponse> getDoubt(
            @PathVariable UUID doubtTicketId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String userRole) {
        DoubtTicketResponse response = getDoubtUseCase.getDoubt(doubtTicketId, userId);
        return ResponseEntity.ok(response);
    }
}
