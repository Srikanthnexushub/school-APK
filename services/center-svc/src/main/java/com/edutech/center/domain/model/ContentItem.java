package com.edutech.center.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * Study material / document item in the center's content library.
 *
 * Files are stored in MinIO (S3-compatible). minioObjectKey + bucketName are
 * the canonical storage identifiers; presigned download URLs are generated
 * on-demand by DocumentStoragePort and never persisted here.
 *
 * Soft-deleted via deletedAt. Status lifecycle:
 *   PROCESSING → AVAILABLE (after AI auto-tagging via Kafka consumer)
 *   AVAILABLE  → ARCHIVED  (soft-delete via archive())
 */
@Entity
@Table(name = "content_items", schema = "center_schema")
public class ContentItem {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "center_id", updatable = false)
    private UUID centerId;

    @Column(name = "batch_id")
    private UUID batchId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ContentType type;

    @Column(name = "file_url", length = 2000)
    private String fileUrl;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "uploaded_by_user_id", updatable = false, nullable = false)
    private UUID uploadedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContentStatus status;

    // ── Library metadata ──────────────────────────────────────────────────────

    @Column(length = 100)
    private String subject;

    @Column(length = 50)
    private String board;

    @Column(name = "class_grade", length = 20)
    private String classGrade;

    @Column(name = "year_of_paper")
    private Short yearOfPaper;

    @Enumerated(EnumType.STRING)
    @Column(name = "exam_type", length = 30)
    private ExamType examType;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Difficulty difficulty;

    @Column(length = 30)
    private String language;

    @Column(columnDefinition = "TEXT[]")
    private String[] tags;

    @Column(name = "download_count", nullable = false)
    private long downloadCount;

    // ── AI-generated fields ───────────────────────────────────────────────────

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    // ── Storage fields ────────────────────────────────────────────────────────

    @Column(name = "thumbnail_url", length = 2000)
    private String thumbnailUrl;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "minio_object_key", length = 1000)
    private String minioObjectKey;

    @Column(name = "bucket_name", length = 200)
    private String bucketName;

    @Column(name = "is_platform_resource", nullable = false)
    private boolean platformResource;

    @Column(name = "stream", length = 30)
    private String stream;

    // ── Audit ─────────────────────────────────────────────────────────────────

    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private Long version;

    protected ContentItem() {}

    private ContentItem(UUID id, UUID centerId, UUID batchId, String title,
                        String description, ContentType type, String fileUrl,
                        Long fileSizeBytes, UUID uploadedByUserId,
                        String subject, String board, String classGrade,
                        Short yearOfPaper, ExamType examType, Difficulty difficulty,
                        String language, String[] tags, String thumbnailUrl,
                        String mimeType, Integer pageCount,
                        String minioObjectKey, String bucketName,
                        boolean platformResource, String stream) {
        this.id = id;
        this.centerId = centerId;
        this.batchId = batchId;
        this.title = title;
        this.description = description;
        this.type = type;
        this.fileUrl = fileUrl;
        this.fileSizeBytes = fileSizeBytes;
        this.uploadedByUserId = uploadedByUserId;
        this.status = ContentStatus.PROCESSING;
        this.subject = subject;
        this.board = board;
        this.classGrade = classGrade;
        this.yearOfPaper = yearOfPaper;
        this.examType = examType;
        this.difficulty = difficulty;
        this.language = (language != null && !language.isBlank()) ? language : "ENGLISH";
        this.tags = tags;
        this.thumbnailUrl = thumbnailUrl;
        this.mimeType = mimeType;
        this.pageCount = pageCount;
        this.minioObjectKey = minioObjectKey;
        this.bucketName = bucketName;
        this.platformResource = platformResource;
        this.stream = stream;
        this.downloadCount = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /** Legacy factory — keeps existing callers working; status starts as PROCESSING. */
    public static ContentItem create(UUID centerId, UUID batchId, String title,
                                     String description, ContentType type,
                                     String fileUrl, Long fileSizeBytes,
                                     UUID uploadedByUserId) {
        return new ContentItem(UUID.randomUUID(), centerId, batchId, title,
                description, type, fileUrl, fileSizeBytes, uploadedByUserId,
                null, null, null, null, null, null, "ENGLISH", null,
                null, null, null, null, null, false, null);
    }

    /** Full factory used by the presigned-upload confirm flow. */
    public static ContentItem createWithMetadata(UUID centerId, UUID batchId, String title,
                                                  String description, ContentType type,
                                                  Long fileSizeBytes, UUID uploadedByUserId,
                                                  String subject, String board, String classGrade,
                                                  Short yearOfPaper, ExamType examType,
                                                  Difficulty difficulty, String language,
                                                  String[] tags, String thumbnailUrl,
                                                  String mimeType, Integer pageCount,
                                                  String minioObjectKey, String bucketName) {
        return new ContentItem(UUID.randomUUID(), centerId, batchId, title,
                description, type, null, fileSizeBytes, uploadedByUserId,
                subject, board, classGrade, yearOfPaper, examType, difficulty,
                language, tags, thumbnailUrl, mimeType, pageCount,
                minioObjectKey, bucketName, false, null);
    }

    /**
     * Factory for external link/YouTube content.
     * Links go directly to AVAILABLE — no MinIO upload needed.
     * fileUrl holds the external URL.
     * isPlatformResource=false means center-scoped; use createPlatformLink for global.
     */
    public static ContentItem createLink(UUID centerId, String title, String description,
                                          ContentType type, String externalUrl,
                                          UUID uploadedByUserId, String subject, String board,
                                          String classGrade, String stream, ExamType examType,
                                          Difficulty difficulty, String language, String[] tags,
                                          String thumbnailUrl) {
        ContentItem item = new ContentItem(UUID.randomUUID(), centerId, null, title,
                description, type, externalUrl, null, uploadedByUserId,
                subject, board, classGrade, null, examType, difficulty,
                language, tags, thumbnailUrl, null, null, null, null,
                false, stream);
        item.status = ContentStatus.AVAILABLE; // links are immediately available
        return item;
    }

    /**
     * Factory for SUPER_ADMIN / INSTITUTION_ADMIN platform-wide links.
     * centerId is null — visible to all students matching the profile filters.
     */
    public static ContentItem createPlatformLink(String title, String description,
                                                  ContentType type, String externalUrl,
                                                  UUID uploadedByUserId, String subject, String board,
                                                  String classGrade, String stream, ExamType examType,
                                                  Difficulty difficulty, String language, String[] tags,
                                                  String thumbnailUrl) {
        ContentItem item = new ContentItem(UUID.randomUUID(), null, null, title,
                description, type, externalUrl, null, uploadedByUserId,
                subject, board, classGrade, null, examType, difficulty,
                language, tags, thumbnailUrl, null, null, null, null,
                true, stream);
        item.status = ContentStatus.AVAILABLE;
        return item;
    }

    public void markAvailable() {
        this.status = ContentStatus.AVAILABLE;
        this.updatedAt = Instant.now();
    }

    public void archive() {
        this.status = ContentStatus.ARCHIVED;
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Applies AI-generated tags to this item and transitions status to AVAILABLE.
     * Safely ignores unknown enum values (graceful degradation).
     */
    public void applyAiTags(String aiSummary, String subject, String board,
                             String examType, String difficulty, String[] tags) {
        this.aiSummary = aiSummary;
        if (subject != null && !subject.isBlank()) this.subject = subject;
        if (board != null && !board.isBlank()) this.board = board;
        if (examType != null && !examType.isBlank()) {
            try { this.examType = ExamType.valueOf(examType.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }
        if (difficulty != null && !difficulty.isBlank()) {
            try { this.difficulty = Difficulty.valueOf(difficulty.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }
        if (tags != null && tags.length > 0) this.tags = tags;
        this.status = ContentStatus.AVAILABLE;
        this.updatedAt = Instant.now();
    }

    public void incrementDownloadCount() {
        this.downloadCount++;
        this.updatedAt = Instant.now();
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public UUID getCenterId() { return centerId; }
    public UUID getBatchId() { return batchId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public ContentType getType() { return type; }
    public String getFileUrl() { return fileUrl; }
    public Long getFileSizeBytes() { return fileSizeBytes; }
    public UUID getUploadedByUserId() { return uploadedByUserId; }
    public ContentStatus getStatus() { return status; }
    public String getSubject() { return subject; }
    public String getBoard() { return board; }
    public String getClassGrade() { return classGrade; }
    public Short getYearOfPaper() { return yearOfPaper; }
    public ExamType getExamType() { return examType; }
    public Difficulty getDifficulty() { return difficulty; }
    public String getLanguage() { return language; }
    public String[] getTags() { return tags; }
    public long getDownloadCount() { return downloadCount; }
    public String getAiSummary() { return aiSummary; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getMimeType() { return mimeType; }
    public Integer getPageCount() { return pageCount; }
    public String getMinioObjectKey() { return minioObjectKey; }
    public String getBucketName() { return bucketName; }
    public boolean isPlatformResource() { return platformResource; }
    public String getStream() { return stream; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public Long getVersion() { return version; }
}
