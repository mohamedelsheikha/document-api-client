package com.claims.documentapi.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MultipartUploadStatusResponse {
    private String sessionId;
    private String documentId;
    private String uploadId;
    private String s3Key;
    private String bucket;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private Integer partSizeBytes;
    private String status;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String attachmentId;
}
