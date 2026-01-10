package com.claims.documentapi.dto;

import lombok.Data;

@Data
public class MultipartUploadCompleteResponse {
    private String sessionId;
    private String attachmentId;
    private String s3Key;
    private String bucket;
}
