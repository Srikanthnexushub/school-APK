// src/main/java/com/edutech/center/application/service/ContentService.java
package com.edutech.center.application.service;

import com.edutech.center.application.dto.AuthPrincipal;
import com.edutech.center.application.dto.ContentItemResponse;
import com.edutech.center.application.dto.UploadContentRequest;
import com.edutech.center.application.exception.CenterAccessDeniedException;
import com.edutech.center.application.exception.CenterNotFoundException;
import com.edutech.center.domain.event.ContentUploadedEvent;
import com.edutech.center.domain.model.ContentItem;
import com.edutech.center.domain.port.in.UploadContentUseCase;
import com.edutech.center.domain.port.out.CenterEventPublisher;
import com.edutech.center.domain.port.out.CenterRepository;
import com.edutech.center.domain.port.out.ContentRepository;
import com.edutech.center.domain.port.out.TeacherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ContentService implements UploadContentUseCase {

    private static final Logger log = LoggerFactory.getLogger(ContentService.class);

    private final ContentRepository contentRepository;
    private final CenterRepository centerRepository;
    private final CenterEventPublisher eventPublisher;
    private final TeacherRepository teacherRepository;

    public ContentService(ContentRepository contentRepository,
                          CenterRepository centerRepository,
                          CenterEventPublisher eventPublisher,
                          TeacherRepository teacherRepository) {
        this.contentRepository = contentRepository;
        this.centerRepository = centerRepository;
        this.eventPublisher = eventPublisher;
        this.teacherRepository = teacherRepository;
    }

    /** True if principal is super-admin, center owner, an assigned teacher, or a student (read-only). */
    private boolean hasAccess(AuthPrincipal principal, UUID centerId) {
        return principal.belongsToCenter(centerId)
                || teacherRepository.existsByUserIdAndCenterId(principal.userId(), centerId)
                || principal.isStudent();
    }

    @Override
    @Transactional
    public ContentItemResponse uploadContent(UUID centerId, UploadContentRequest request,
                                             AuthPrincipal principal) {
        if (!hasAccess(principal, centerId)) {
            throw new CenterAccessDeniedException();
        }
        centerRepository.findById(centerId)
            .orElseThrow(() -> new CenterNotFoundException(centerId));

        ContentItem item = ContentItem.create(centerId, request.batchId(),
                request.title(), request.description(), request.type(),
                request.fileUrl(), request.fileSizeBytes(), principal.userId());

        ContentItem saved = contentRepository.save(item);

        eventPublisher.publish(new ContentUploadedEvent(
                saved.getId(), centerId, saved.getBatchId(),
                saved.getTitle(), saved.getType(), principal.userId()));

        log.info("Content uploaded: id={} centerId={} type={}", saved.getId(), centerId, saved.getType());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ContentItemResponse> listContent(UUID centerId, AuthPrincipal principal) {
        if (!hasAccess(principal, centerId)) {
            throw new CenterAccessDeniedException();
        }
        return contentRepository.findByCenterId(centerId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<ContentItemResponse> listContent(UUID centerId, AuthPrincipal principal, Pageable pageable) {
        if (!hasAccess(principal, centerId)) {
            throw new CenterAccessDeniedException();
        }
        List<ContentItemResponse> all = contentRepository.findByCenterId(centerId).stream()
                .map(this::toResponse).toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        return new PageImpl<>(start < all.size() ? all.subList(start, end) : List.of(), pageable, all.size());
    }

    @Transactional(readOnly = true)
    public List<ContentItemResponse> listContentByBatch(UUID batchId, AuthPrincipal principal) {
        return contentRepository.findByBatchId(batchId).stream()
            .filter(c -> hasAccess(principal, c.getCenterId()))
            .map(this::toResponse).toList();
    }

    private ContentItemResponse toResponse(ContentItem c) {
        return new ContentItemResponse(c.getId(), c.getCenterId(), c.getBatchId(),
                c.getTitle(), c.getDescription(), c.getType(), c.getFileUrl(),
                c.getFileSizeBytes(), c.getUploadedByUserId(), c.getStatus(), c.getCreatedAt());
    }
}
