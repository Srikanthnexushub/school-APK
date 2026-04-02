package com.edutech.center.api;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.ContentItemResponse;
import com.edutech.center.application.dto.RegisterLinkRequest;
import com.edutech.center.application.service.ContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Platform-level content management — SUPER_ADMIN and INSTITUTION_ADMIN only.
 * Platform links have no centerId; they are visible to all students
 * whose profile (board + classGrade + stream) matches the content metadata.
 */
@RestController
@RequestMapping("/api/v1/platform/content")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Platform Content", description = "Platform-wide study resources — SUPER_ADMIN / INSTITUTION_ADMIN")
public class PlatformContentController {

    private final ContentService contentService;

    public PlatformContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @PostMapping("/link")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Publish a platform-wide external link visible to all students matching the resource profile")
    public ContentItemResponse registerPlatformLink(@Valid @RequestBody RegisterLinkRequest request,
                                                     @AuthenticationPrincipal AuthPrincipal principal) {
        return contentService.registerPlatformLink(request, principal);
    }
}
