package com.edutech.center.domain.port.out;

import java.util.UUID;

/**
 * Port for object storage operations (MinIO / S3-compatible).
 *
 * Implementations generate presigned URLs for direct browser-to-storage uploads
 * and time-limited download URLs. No file data passes through the service layer.
 */
public interface DocumentStoragePort {

    /**
     * Generates a presigned HTTP PUT URL that allows the browser to upload
     * a file directly to MinIO without routing through the service.
     *
     * @param objectKey     the target object key in the bucket
     * @param mimeType      the Content-Type the browser will send
     * @param expiryMinutes how long the URL remains valid
     * @return a presigned PUT URL
     */
    String generateUploadUrl(String objectKey, String mimeType, int expiryMinutes);

    /**
     * Generates a time-limited presigned HTTP GET URL for downloading a stored object.
     *
     * @param objectKey     the object key in the bucket
     * @param expiryMinutes how long the URL remains valid
     * @return a presigned GET URL
     */
    String generateDownloadUrl(String objectKey, int expiryMinutes);

    /**
     * Builds a deterministic, center-scoped object key.
     * Pattern: {centerId}/{contentType}/{randomUUID}/{sanitisedFilename}
     */
    String buildObjectKey(UUID centerId, String contentType, String filename);

    /**
     * Ensures the configured bucket exists, creating it if necessary.
     * Called once at application startup.
     */
    void ensureBucketExists(String bucketName);
}
