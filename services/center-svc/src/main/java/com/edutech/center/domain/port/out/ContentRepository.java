package com.edutech.center.domain.port.out;

import com.edutech.center.domain.model.ContentItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentRepository {

    ContentItem save(ContentItem item);

    Optional<ContentItem> findById(UUID id);

    Optional<ContentItem> findByIdActive(UUID id);

    List<ContentItem> findByCenterId(UUID centerId);

    List<ContentItem> findByBatchId(UUID batchId);

    /** Increments download_count by 1 for the given content item. */
    void incrementDownloadCount(UUID id);

    /** Full-text + filter search across all non-deleted content. */
    Page<ContentItem> search(String query, String subject, String board,
                              String classGrade, Short yearOfPaper,
                              String examType, String difficulty,
                              String language, String contentType,
                              UUID centerId, Pageable pageable);

    /** Top downloaded content, ordered by download_count DESC. */
    List<ContentItem> findTrending(Pageable pageable);

    /** Recently added content since the given instant. */
    List<ContentItem> findRecentSince(Instant since, Pageable pageable);
}
