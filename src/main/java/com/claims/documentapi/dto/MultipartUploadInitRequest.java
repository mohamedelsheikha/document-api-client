package com.claims.documentapi.dto;

import lombok.Data;

@Data
public class MultipartUploadInitRequest {
    private String fileName;
    private String contentType;
    private Long fileSize;
    private Integer partSizeBytes;
}
