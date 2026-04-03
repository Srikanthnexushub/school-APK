package com.edutech.center.infrastructure.storage;

import com.edutech.center.domain.port.out.DocumentStoragePort;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class MinioStorageAdapter implements DocumentStoragePort {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageAdapter.class);

    private final MinioClient minioClient;
    private final String bucketName;

    public MinioStorageAdapter(MinioClient minioClient,
                               @Value("${minio.bucket-name}") String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
    }

    @PostConstruct
    public void init() {
        ensureBucketExists(bucketName);
    }

    @Override
    public String generateUploadUrl(String objectKey, String mimeType, int expiryMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucketName)
                            .object(objectKey)
                            .expiry(expiryMinutes, TimeUnit.MINUTES)
                            .build());
        } catch (Exception e) {
            log.error("Failed to generate presigned upload URL for objectKey={}", objectKey, e);
            throw new RuntimeException("Could not generate upload URL", e);
        }
    }

    @Override
    public String generateDownloadUrl(String objectKey, int expiryMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectKey)
                            .expiry(expiryMinutes, TimeUnit.MINUTES)
                            .build());
        } catch (Exception e) {
            log.error("Failed to generate presigned download URL for objectKey={}", objectKey, e);
            throw new RuntimeException("Could not generate download URL", e);
        }
    }

    @Override
    public void putObject(String objectKey, InputStream data, long size, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(data, size, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            log.error("Failed to put object objectKey={}", objectKey, e);
            throw new RuntimeException("Could not upload file to storage", e);
        }
    }

    @Override
    public String buildObjectKey(UUID centerId, String contentType, String filename) {
        String sanitised = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return centerId + "/" + contentType.toLowerCase() + "/" + UUID.randomUUID() + "/" + sanitised;
    }

    @Override
    public void ensureBucketExists(String bucket) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created MinIO bucket '{}'", bucket);
            } else {
                log.debug("MinIO bucket '{}' already exists", bucket);
            }
        } catch (Exception e) {
            log.error("Failed to ensure MinIO bucket '{}' exists", bucket, e);
            throw new RuntimeException("Could not initialise storage bucket: " + bucket, e);
        }
    }
}
