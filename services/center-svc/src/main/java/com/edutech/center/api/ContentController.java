package com.edutech.center.api;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.ConfirmUploadRequest;
import com.edutech.center.application.dto.ContentItemResponse;
import com.edutech.center.application.dto.PresignedUploadResponse;
import com.edutech.center.application.dto.UploadContentRequest;
import com.edutech.center.application.service.ContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/centers/{centerId}/content")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Content Library", description = "Intelligent document library — previous year papers, study materials")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    // ── Legacy endpoint (kept for backward compatibility) ─────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register content metadata for a file already on CDN/S3 (legacy)")
    public ContentItemResponse uploadContent(@PathVariable UUID centerId,
                                             @Valid @RequestBody UploadContentRequest request,
                                             @AuthenticationPrincipal AuthPrincipal principal) {
        return contentService.uploadContent(centerId, request, principal);
    }

    // ── Presigned MinIO upload flow ───────────────────────────────────────────

    @GetMapping("/presigned-upload")
    @Operation(summary = "Get a presigned PUT URL for direct browser → MinIO upload")
    public PresignedUploadResponse getPresignedUploadUrl(
            @PathVariable UUID centerId,
            @RequestParam String filename,
            @RequestParam(defaultValue = "application/octet-stream") String mimeType,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return contentService.generatePresignedUploadUrl(centerId, filename, mimeType, principal);
    }

    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Confirm a completed MinIO upload and persist metadata")
    public ContentItemResponse confirmUpload(@PathVariable UUID centerId,
                                              @Valid @RequestBody ConfirmUploadRequest request,
                                              @AuthenticationPrincipal AuthPrincipal principal) {
        return contentService.confirmUpload(centerId, request, principal);
    }

    // ── Download ──────────────────────────────────────────────────────────────

    @GetMapping("/{contentId}/download")
    @Operation(summary = "Get a time-limited presigned download URL; increments download counter")
    public Map<String, String> getDownloadUrl(@PathVariable UUID centerId,
                                               @PathVariable UUID contentId,
                                               @AuthenticationPrincipal AuthPrincipal principal) {
        String url = contentService.generateDownloadUrl(centerId, contentId, principal);
        return Map.of("downloadUrl", url);
    }

    // ── Archive ───────────────────────────────────────────────────────────────

    @DeleteMapping("/{contentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-archive (soft-delete) a content item")
    public void archiveContent(@PathVariable UUID centerId,
                                @PathVariable UUID contentId,
                                @AuthenticationPrincipal AuthPrincipal principal) {
        contentService.archiveContent(centerId, contentId, principal);
    }

    // ── AI re-tag ─────────────────────────────────────────────────────────────

    @PostMapping("/{contentId}/ai-tag")
    @Operation(summary = "Trigger AI auto-tagging for a content item (manual re-run)")
    public ContentItemResponse reTag(@PathVariable UUID centerId,
                                      @PathVariable UUID contentId,
                                      @AuthenticationPrincipal AuthPrincipal principal) {
        return contentService.reTag(centerId, contentId, principal);
    }

    // ── List ──────────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List content items for a center (paginated)")
    public Page<ContentItemResponse> listContent(@PathVariable UUID centerId,
                                                 @AuthenticationPrincipal AuthPrincipal principal,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "50") int size) {
        return contentService.listContent(centerId, principal, PageRequest.of(page, size));
    }
}
