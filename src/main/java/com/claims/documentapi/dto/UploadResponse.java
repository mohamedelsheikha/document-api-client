package com.claims.documentapi.dto;

import lombok.Data;

@Data
public class UploadResponse {
    private String status;
    private String message;
    private String attachmentId;
    private String documentId;
    private String fileName;
    private long fileSize;
}
