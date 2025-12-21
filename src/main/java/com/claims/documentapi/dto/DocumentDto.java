package com.claims.documentapi.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class DocumentDto {
    private String id; // Database attachment ID
    private String documentId; // Document reference
    private String fileName;
    private String originalFileName;
    private String contentType;
    private long fileSize;
    private String s3Key;
    private String s3Bucket;
    private String uploadedBy;

    private String presignedUrl;
    private Instant presignedUrlExpiresAt;
}
