package com.edutech.center.api;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.ContentItemResponse;
import com.edutech.center.application.service.LibraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Global platform-wide library search and discovery.
 * Open to all authenticated roles — no center scoping required.
 * Routes through api-gateway at /api/v1/library/**.
 */
@RestController
@RequestMapping("/api/v1/library")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Document Library — Discovery", description = "Platform-wide search across all centers")
public class LibraryController {

    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @GetMapping("/search")
    @Operation(summary = "Full-text + filter search across all centers")
    public Page<ContentItemResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String board,
            @RequestParam(required = false) String classGrade,
            @RequestParam(required = false) Short yearOfPaper,
            @RequestParam(required = false) String examType,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String contentType,
            @RequestParam(required = false) UUID centerId,
            @RequestParam(required = false) String stream,
            @RequestParam(required = false) Boolean platformOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return libraryService.search(q, subject, board, classGrade, yearOfPaper,
                examType, difficulty, language, contentType, centerId, stream, platformOnly,
                PageRequest.of(page, size));
    }

    @GetMapping("/trending")
    @Operation(summary = "Top downloaded documents across the platform")
    public List<ContentItemResponse> trending(
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return libraryService.trending(PageRequest.of(0, size));
    }

    @GetMapping("/recent")
    @Operation(summary = "Recently added documents (last N days)")
    public List<ContentItemResponse> recent(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return libraryService.recent(days, PageRequest.of(0, size));
    }
}
