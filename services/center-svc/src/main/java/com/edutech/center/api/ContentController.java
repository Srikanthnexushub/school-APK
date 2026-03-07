// src/main/java/com/edutech/center/api/ContentController.java
package com.edutech.center.api;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.ContentItemResponse;
import com.edutech.center.application.dto.UploadContentRequest;
import com.edutech.center.application.service.ContentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/centers/{centerId}/content")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Content Library", description = "Study material management")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register uploaded content metadata (file already on CDN/S3)")
    public ContentItemResponse uploadContent(@PathVariable UUID centerId,
                                             @Valid @RequestBody UploadContentRequest request,
                                             @AuthenticationPrincipal AuthPrincipal principal) {
        return contentService.uploadContent(centerId, request, principal);
    }

    @GetMapping
    @Operation(summary = "List content items for a center")
    public List<ContentItemResponse> listContent(@PathVariable UUID centerId,
                                                 @AuthenticationPrincipal AuthPrincipal principal) {
        return contentService.listContent(centerId, principal);
    }
}
