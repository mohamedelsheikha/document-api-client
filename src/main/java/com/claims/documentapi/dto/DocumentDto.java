package com.claims.documentapi.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class DocumentDto {
    private String id;
    private String documentId;
    private String fileName;
    private String fileType;
    private long fileSize;

    private String presignedUrl;
    private Instant presignedUrlExpiresAt;
}
