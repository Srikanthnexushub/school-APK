// src/main/java/com/edutech/center/domain/model/ContentItem.java
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
 * Study material item in the center's content library.
 * fileUrl is a pre-signed S3 URL or CDN URL — file is stored externally.
 * Soft-deleted via deletedAt; status tracks processing lifecycle.
 */
@Entity
@Table(name = "content_items", schema = "center_schema")
public class ContentItem {

    @Id
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "center_id", updatable = false, nullable = false)
    private UUID centerId;

    @Column(name = "batch_id")
    private UUID batchId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContentType type;

    @Column(name = "file_url", nullable = false, length = 2000)
    private String fileUrl;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "uploaded_by_user_id", updatable = false, nullable = false)
    private UUID uploadedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContentStatus status;

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
                        Long fileSizeBytes, UUID uploadedByUserId) {
        this.id = id;
        this.centerId = centerId;
        this.batchId = batchId;
        this.title = title;
        this.description = description;
        this.type = type;
        this.fileUrl = fileUrl;
        this.fileSizeBytes = fileSizeBytes;
        this.uploadedByUserId = uploadedByUserId;
        this.status = ContentStatus.AVAILABLE;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public static ContentItem create(UUID centerId, UUID batchId, String title,
                                     String description, ContentType type,
                                     String fileUrl, Long fileSizeBytes,
                                     UUID uploadedByUserId) {
        return new ContentItem(UUID.randomUUID(), centerId, batchId, title,
                description, type, fileUrl, fileSizeBytes, uploadedByUserId);
    }

    public void archive() {
        this.status = ContentStatus.ARCHIVED;
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

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
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public Long getVersion() { return version; }
}
