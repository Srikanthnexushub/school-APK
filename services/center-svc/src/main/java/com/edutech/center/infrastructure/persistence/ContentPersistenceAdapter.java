package com.edutech.center.infrastructure.persistence;

import com.edutech.center.domain.model.ContentItem;
import com.edutech.center.domain.port.out.ContentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ContentPersistenceAdapter implements ContentRepository {

    private final SpringDataContentRepository jpa;

    public ContentPersistenceAdapter(SpringDataContentRepository jpa) { this.jpa = jpa; }

    @Override public ContentItem save(ContentItem item) { return jpa.save(item); }
    @Override public Optional<ContentItem> findById(UUID id) { return jpa.findById(id); }
    @Override public Optional<ContentItem> findByIdActive(UUID id) { return jpa.findByIdActive(id); }
    @Override public List<ContentItem> findByCenterId(UUID centerId) { return jpa.findByCenterIdActive(centerId); }
    @Override public List<ContentItem> findByBatchId(UUID batchId) { return jpa.findByBatchIdActive(batchId); }

    @Override
    @Transactional
    public void incrementDownloadCount(UUID id) { jpa.incrementDownloadCount(id); }

    @Override
    public Page<ContentItem> search(String query, String subject, String board,
                                     String classGrade, Short yearOfPaper,
                                     String examType, String difficulty,
                                     String language, String contentType,
                                     UUID centerId, String stream, Boolean platformOnly,
                                     Pageable pageable) {
        return jpa.searchWithFilters(
                (query == null || query.isBlank()) ? null : query,
                subject, board, classGrade, yearOfPaper,
                examType, difficulty, language, contentType, centerId,
                stream, platformOnly, pageable);
    }

    @Override
    public List<ContentItem> findTrending(Pageable pageable) {
        return jpa.findTrending(pageable);
    }

    @Override
    public List<ContentItem> findRecentSince(Instant since, Pageable pageable) {
        return jpa.findRecentSince(since, pageable);
    }
}
