package com.claims.documentapi.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentAttachmentDto {
    private String id;
    private String documentId;
    private String fileName;
    private String originalFileName;
    private String contentType;
    private long fileSize;
    private String s3Key;
    private String s3Bucket;
    private String uploadedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
