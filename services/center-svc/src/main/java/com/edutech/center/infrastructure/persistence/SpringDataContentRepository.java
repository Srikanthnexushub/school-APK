package com.edutech.center.infrastructure.persistence;

import com.edutech.center.domain.model.ContentItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataContentRepository extends JpaRepository<ContentItem, UUID> {

    @Query("SELECT c FROM ContentItem c WHERE c.centerId = :centerId AND c.deletedAt IS NULL ORDER BY c.createdAt DESC")
    List<ContentItem> findByCenterIdActive(@Param("centerId") UUID centerId);

    @Query("SELECT c FROM ContentItem c WHERE c.batchId = :batchId AND c.deletedAt IS NULL ORDER BY c.createdAt DESC")
    List<ContentItem> findByBatchIdActive(@Param("batchId") UUID batchId);

    @Query("SELECT c FROM ContentItem c WHERE c.id = :id AND c.deletedAt IS NULL")
    Optional<ContentItem> findByIdActive(@Param("id") UUID id);

    @Modifying
    @Query("UPDATE ContentItem c SET c.downloadCount = c.downloadCount + 1, c.updatedAt = CURRENT_TIMESTAMP WHERE c.id = :id")
    void incrementDownloadCount(@Param("id") UUID id);

    /**
     * Combined FTS + filter search.
     * Uses native SQL for the plainto_tsquery FTS operator.
     * CAST(:param AS VARCHAR) prevents lower(bytea) error on nullable params in Postgres.
     * When :query is blank, skips FTS and returns all matching filters.
     */
    @Query(value = """
            SELECT * FROM center_schema.content_items c
            WHERE c.deleted_at IS NULL
              AND (:query IS NULL OR :query = '' OR c.fts_vector @@ plainto_tsquery('english', :query))
              AND (CAST(:subject AS VARCHAR) IS NULL OR c.subject = CAST(:subject AS VARCHAR))
              AND (CAST(:board AS VARCHAR) IS NULL OR c.board = CAST(:board AS VARCHAR))
              AND (CAST(:classGrade AS VARCHAR) IS NULL OR c.class_grade = CAST(:classGrade AS VARCHAR))
              AND (:yearOfPaper IS NULL OR c.year_of_paper = :yearOfPaper)
              AND (CAST(:examType AS VARCHAR) IS NULL OR c.exam_type = CAST(:examType AS VARCHAR))
              AND (CAST(:difficulty AS VARCHAR) IS NULL OR c.difficulty = CAST(:difficulty AS VARCHAR))
              AND (CAST(:language AS VARCHAR) IS NULL OR c.language = CAST(:language AS VARCHAR))
              AND (CAST(:contentType AS VARCHAR) IS NULL OR c.type = CAST(:contentType AS VARCHAR))
              AND (:centerId IS NULL OR c.center_id = :centerId)
              AND (CAST(:stream AS VARCHAR) IS NULL OR c.stream = CAST(:stream AS VARCHAR))
              AND (:platformOnly IS NULL OR :platformOnly = FALSE OR c.is_platform_resource = TRUE)
            ORDER BY c.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM center_schema.content_items c
            WHERE c.deleted_at IS NULL
              AND (:query IS NULL OR :query = '' OR c.fts_vector @@ plainto_tsquery('english', :query))
              AND (CAST(:subject AS VARCHAR) IS NULL OR c.subject = CAST(:subject AS VARCHAR))
              AND (CAST(:board AS VARCHAR) IS NULL OR c.board = CAST(:board AS VARCHAR))
              AND (CAST(:classGrade AS VARCHAR) IS NULL OR c.class_grade = CAST(:classGrade AS VARCHAR))
              AND (:yearOfPaper IS NULL OR c.year_of_paper = :yearOfPaper)
              AND (CAST(:examType AS VARCHAR) IS NULL OR c.exam_type = CAST(:examType AS VARCHAR))
              AND (CAST(:difficulty AS VARCHAR) IS NULL OR c.difficulty = CAST(:difficulty AS VARCHAR))
              AND (CAST(:language AS VARCHAR) IS NULL OR c.language = CAST(:language AS VARCHAR))
              AND (CAST(:contentType AS VARCHAR) IS NULL OR c.type = CAST(:contentType AS VARCHAR))
              AND (:centerId IS NULL OR c.center_id = :centerId)
              AND (CAST(:stream AS VARCHAR) IS NULL OR c.stream = CAST(:stream AS VARCHAR))
              AND (:platformOnly IS NULL OR :platformOnly = FALSE OR c.is_platform_resource = TRUE)
            """,
            nativeQuery = true)
    Page<ContentItem> searchWithFilters(
            @Param("query") String query,
            @Param("subject") String subject,
            @Param("board") String board,
            @Param("classGrade") String classGrade,
            @Param("yearOfPaper") Short yearOfPaper,
            @Param("examType") String examType,
            @Param("difficulty") String difficulty,
            @Param("language") String language,
            @Param("contentType") String contentType,
            @Param("centerId") UUID centerId,
            @Param("stream") String stream,
            @Param("platformOnly") Boolean platformOnly,
            Pageable pageable);

    @Query(value = """
            SELECT * FROM center_schema.content_items
            WHERE deleted_at IS NULL
            ORDER BY download_count DESC
            """, nativeQuery = true)
    List<ContentItem> findTrending(Pageable pageable);

    @Query(value = """
            SELECT * FROM center_schema.content_items
            WHERE deleted_at IS NULL AND created_at >= :since
            ORDER BY created_at DESC
            """, nativeQuery = true)
    List<ContentItem> findRecentSince(@Param("since") Instant since, Pageable pageable);
}
