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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public ContentService(ContentRepository contentRepository,
                          CenterRepository centerRepository,
                          CenterEventPublisher eventPublisher) {
        this.contentRepository = contentRepository;
        this.centerRepository = centerRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public ContentItemResponse uploadContent(UUID centerId, UploadContentRequest request,
                                             AuthPrincipal principal) {
        if (!principal.belongsToCenter(centerId)) {
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
        if (!principal.belongsToCenter(centerId)) {
            throw new CenterAccessDeniedException();
        }
        return contentRepository.findByCenterId(centerId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ContentItemResponse> listContentByBatch(UUID batchId, AuthPrincipal principal) {
        return contentRepository.findByBatchId(batchId).stream()
            .filter(c -> principal.belongsToCenter(c.getCenterId()))
            .map(this::toResponse).toList();
    }

    private ContentItemResponse toResponse(ContentItem c) {
        return new ContentItemResponse(c.getId(), c.getCenterId(), c.getBatchId(),
                c.getTitle(), c.getDescription(), c.getType(), c.getFileUrl(),
                c.getFileSizeBytes(), c.getUploadedByUserId(), c.getStatus(), c.getCreatedAt());
    }
}
