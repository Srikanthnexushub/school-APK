package com.edutech.center.application.service;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.ConfirmUploadRequest;
import com.edutech.center.application.dto.ContentItemResponse;
import com.edutech.center.application.dto.PresignedUploadResponse;
import com.edutech.center.application.dto.RegisterLinkRequest;
import com.edutech.center.application.dto.UploadContentRequest;
import com.edutech.center.application.exception.CenterAccessDeniedException;
import com.edutech.center.application.exception.CenterNotFoundException;
import com.edutech.center.domain.event.ContentUploadedEvent;
import com.edutech.center.domain.model.ContentItem;
import com.edutech.center.domain.model.ContentType;
import com.edutech.center.domain.port.in.UploadContentUseCase;
import com.edutech.center.domain.port.out.CenterEventPublisher;
import com.edutech.center.domain.port.out.CenterRepository;
import com.edutech.center.domain.port.out.ContentRepository;
import com.edutech.center.domain.model.TaggingResult;
import com.edutech.center.domain.port.out.AiTaggingPort;
import com.edutech.center.domain.port.out.DocumentStoragePort;
import com.edutech.center.domain.port.out.TeacherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class ContentService implements UploadContentUseCase {

    private static final Logger log = LoggerFactory.getLogger(ContentService.class);

    private final ContentRepository contentRepository;
    private final CenterRepository centerRepository;
    private final CenterEventPublisher eventPublisher;
    private final TeacherRepository teacherRepository;
    private final DocumentStoragePort storagePort;
    private final AiTaggingPort aiTaggingPort;
    private final int uploadExpiryMinutes;
    private final int downloadExpiryMinutes;

    public ContentService(ContentRepository contentRepository,
                          CenterRepository centerRepository,
                          CenterEventPublisher eventPublisher,
                          TeacherRepository teacherRepository,
                          DocumentStoragePort storagePort,
                          AiTaggingPort aiTaggingPort,
                          @Value("${minio.presigned-upload-expiry-minutes}") int uploadExpiryMinutes,
                          @Value("${minio.presigned-download-expiry-minutes}") int downloadExpiryMinutes) {
        this.contentRepository = contentRepository;
        this.centerRepository = centerRepository;
        this.eventPublisher = eventPublisher;
        this.teacherRepository = teacherRepository;
        this.storagePort = storagePort;
        this.aiTaggingPort = aiTaggingPort;
        this.uploadExpiryMinutes = uploadExpiryMinutes;
        this.downloadExpiryMinutes = downloadExpiryMinutes;
    }

    /** True if principal can read/write content for this center. */
    private boolean hasAccess(AuthPrincipal principal, UUID centerId) {
        if (principal.belongsToCenter(centerId)) return true;
        if (teacherRepository.existsByUserIdAndCenterId(principal.userId(), centerId)) return true;
        if (principal.isStudent()) return true;
        return centerRepository.findById(centerId)
                .map(c -> principal.belongsToCenter(centerId, c.getAdminUserId()))
                .orElse(false);
    }

    /** True if principal can upload/manage content (not read-only student/parent). */
    private boolean canUpload(AuthPrincipal principal, UUID centerId) {
        if (principal.isSuperAdmin() || principal.isInstitutionAdmin()) return true;
        if (principal.isCenterAdmin() && principal.belongsToCenter(centerId)) return true;
        return teacherRepository.existsByUserIdAndCenterId(principal.userId(), centerId);
    }

    // ── Presigned upload flow ─────────────────────────────────────────────────

    public PresignedUploadResponse generatePresignedUploadUrl(UUID centerId, String filename,
                                                               String mimeType, AuthPrincipal principal) {
        centerRepository.findById(centerId).orElseThrow(() -> new CenterNotFoundException(centerId));
        if (!canUpload(principal, centerId)) throw new CenterAccessDeniedException();
        String objectKey = storagePort.buildObjectKey(centerId, "documents", filename);
        String url = storagePort.generateUploadUrl(objectKey, mimeType, uploadExpiryMinutes);
        return new PresignedUploadResponse(url, objectKey, uploadExpiryMinutes * 60);
    }

    @Transactional
    public ContentItemResponse confirmUpload(UUID centerId, ConfirmUploadRequest request,
                                              AuthPrincipal principal) {
        centerRepository.findById(centerId).orElseThrow(() -> new CenterNotFoundException(centerId));
        if (!canUpload(principal, centerId)) throw new CenterAccessDeniedException();

        String[] tagsArray = request.tags() != null
                ? request.tags().toArray(new String[0]) : null;

        ContentItem item = ContentItem.createWithMetadata(
                centerId, request.batchId(), request.title(), request.description(),
                request.type(), request.fileSizeBytes(), principal.userId(),
                request.subject(), request.board(), request.classGrade(),
                request.yearOfPaper(), request.examType(), request.difficulty(),
                request.language(), tagsArray, request.thumbnailUrl(),
                request.mimeType(), request.pageCount(),
                request.objectKey(), null);

        ContentItem saved = contentRepository.save(item);
        eventPublisher.publish(new ContentUploadedEvent(
                saved.getId(), centerId, saved.getBatchId(),
                saved.getTitle(), saved.getType(), principal.userId()));
        log.info("Content confirmed: id={} centerId={} objectKey={}", saved.getId(), centerId, request.objectKey());
        return toResponse(saved);
    }

    // ── Download ──────────────────────────────────────────────────────────────

    @Transactional
    public String generateDownloadUrl(UUID centerId, UUID contentId, AuthPrincipal principal) {
        if (!hasAccess(principal, centerId)) throw new CenterAccessDeniedException();
        ContentItem item = contentRepository.findByIdActive(contentId)
                .orElseThrow(() -> new NoSuchElementException("Content not found: " + contentId));
        if (!item.getCenterId().equals(centerId)) throw new CenterAccessDeniedException();

        contentRepository.incrementDownloadCount(contentId);
        log.debug("Download requested: contentId={} by userId={}", contentId, principal.userId());

        if (item.getMinioObjectKey() != null && !item.getMinioObjectKey().isBlank()) {
            return storagePort.generateDownloadUrl(item.getMinioObjectKey(), downloadExpiryMinutes);
        }
        // Legacy item — return the stored fileUrl directly
        return item.getFileUrl();
    }

    // ── Archive ───────────────────────────────────────────────────────────────

    @Transactional
    public void archiveContent(UUID centerId, UUID contentId, AuthPrincipal principal) {
        if (!canUpload(principal, centerId)) throw new CenterAccessDeniedException();
        ContentItem item = contentRepository.findByIdActive(contentId)
                .orElseThrow(() -> new NoSuchElementException("Content not found: " + contentId));
        if (!item.getCenterId().equals(centerId)) throw new CenterAccessDeniedException();
        item.archive();
        contentRepository.save(item);
        log.info("Content archived: id={} centerId={}", contentId, centerId);
    }

    // ── Manual AI re-tag ──────────────────────────────────────────────────────

    @Transactional
    public ContentItemResponse reTag(UUID centerId, UUID contentId, AuthPrincipal principal) {
        if (!canUpload(principal, centerId)) throw new CenterAccessDeniedException();
        ContentItem item = contentRepository.findByIdActive(contentId)
                .orElseThrow(() -> new NoSuchElementException("Content not found: " + contentId));
        if (!item.getCenterId().equals(centerId)) throw new CenterAccessDeniedException();
        TaggingResult result = aiTaggingPort.suggestTags(item.getTitle(), item.getDescription());
        item.applyAiTags(result.aiSummary(), result.subject(), result.board(),
                result.examType(), result.difficulty(), result.suggestedTags());
        ContentItem saved = contentRepository.save(item);
        log.info("Manual re-tag done: id={} subject={}", contentId, result.subject());
        return toResponse(saved);
    }

    // ── Link registration ─────────────────────────────────────────────────────

    @Transactional
    public ContentItemResponse registerLink(UUID centerId, RegisterLinkRequest request,
                                             AuthPrincipal principal) {
        centerRepository.findById(centerId).orElseThrow(() -> new CenterNotFoundException(centerId));
        if (!canUpload(principal, centerId)) throw new CenterAccessDeniedException();
        if (request.type() != ContentType.LINK && request.type() != ContentType.VIDEO) {
            throw new IllegalArgumentException("registerLink only accepts LINK or VIDEO types");
        }
        String[] tagsArray = request.tags() != null ? request.tags().toArray(new String[0]) : null;
        ContentItem item = ContentItem.createLink(
                centerId, request.title(), request.description(), request.type(),
                request.externalUrl(), principal.userId(), request.subject(), request.board(),
                request.classGrade(), request.stream(), request.examType(), request.difficulty(),
                request.language(), tagsArray, request.thumbnailUrl());
        ContentItem saved = contentRepository.save(item);
        eventPublisher.publish(new ContentUploadedEvent(
                saved.getId(), centerId, null, saved.getTitle(), saved.getType(), principal.userId()));
        log.info("Link registered: id={} centerId={} url={}", saved.getId(), centerId, request.externalUrl());
        return toResponse(saved);
    }

    @Transactional
    public ContentItemResponse registerPlatformLink(RegisterLinkRequest request,
                                                     AuthPrincipal principal) {
        if (!principal.isSuperAdmin() && !principal.isInstitutionAdmin()) {
            throw new CenterAccessDeniedException();
        }
        if (request.type() != ContentType.LINK && request.type() != ContentType.VIDEO) {
            throw new IllegalArgumentException("registerPlatformLink only accepts LINK or VIDEO types");
        }
        String[] tagsArray = request.tags() != null ? request.tags().toArray(new String[0]) : null;
        ContentItem item = ContentItem.createPlatformLink(
                request.title(), request.description(), request.type(),
                request.externalUrl(), principal.userId(), request.subject(), request.board(),
                request.classGrade(), request.stream(), request.examType(), request.difficulty(),
                request.language(), tagsArray, request.thumbnailUrl());
        ContentItem saved = contentRepository.save(item);
        log.info("Platform link registered: id={} subject={} board={} class={}",
                saved.getId(), request.subject(), request.board(), request.classGrade());
        return toResponse(saved);
    }

    // ── Legacy upload (keep existing callers working) ─────────────────────────

    @Override
    @Transactional
    public ContentItemResponse uploadContent(UUID centerId, UploadContentRequest request,
                                             AuthPrincipal principal) {
        if (!hasAccess(principal, centerId)) throw new CenterAccessDeniedException();
        centerRepository.findById(centerId).orElseThrow(() -> new CenterNotFoundException(centerId));

        String[] tagsArray = request.tags() != null
                ? request.tags().toArray(new String[0]) : null;

        ContentItem item = ContentItem.createWithMetadata(
                centerId, request.batchId(), request.title(), request.description(),
                request.type(), request.fileSizeBytes(), principal.userId(),
                request.subject(), request.board(), request.classGrade(),
                request.yearOfPaper(), request.examType(), request.difficulty(),
                request.language(), tagsArray, request.thumbnailUrl(),
                request.mimeType(), request.pageCount(), null, null);

        // Legacy items with a fileUrl go straight to AVAILABLE
        item.markAvailable();

        ContentItem saved = contentRepository.save(item);
        eventPublisher.publish(new ContentUploadedEvent(
                saved.getId(), centerId, saved.getBatchId(),
                saved.getTitle(), saved.getType(), principal.userId()));
        log.info("Content uploaded (legacy): id={} centerId={}", saved.getId(), centerId);
        return toResponse(saved);
    }

    // ── List ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ContentItemResponse> listContent(UUID centerId, AuthPrincipal principal, Pageable pageable) {
        if (!hasAccess(principal, centerId)) throw new CenterAccessDeniedException();
        List<ContentItemResponse> all = contentRepository.findByCenterId(centerId).stream()
                .map(this::toResponse).toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        return new PageImpl<>(start < all.size() ? all.subList(start, end) : List.of(), pageable, all.size());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    ContentItemResponse toResponse(ContentItem c) {
        return new ContentItemResponse(
                c.getId(), c.getCenterId(), c.getBatchId(),
                c.getTitle(), c.getDescription(), c.getType(),
                c.getFileUrl(), c.getFileSizeBytes(), c.getUploadedByUserId(),
                c.getStatus(), c.getSubject(), c.getBoard(), c.getClassGrade(),
                c.getYearOfPaper(), c.getExamType(), c.getDifficulty(),
                c.getLanguage(), c.getTags(), c.getDownloadCount(),
                c.getAiSummary(), c.getThumbnailUrl(), c.getMimeType(),
                c.getPageCount(), c.getCreatedAt(), c.getUpdatedAt(),
                c.isPlatformResource(), c.getStream());
    }
}
