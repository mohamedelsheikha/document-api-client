package com.claims.documentapi.dto;

import lombok.Data;

@Data
public class MultipartUploadInitResponse {
    private String sessionId;
    private String uploadId;
    private String s3Key;
    private String bucket;
    private Integer partSizeBytes;
}
