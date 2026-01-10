package com.claims.documentapi.dto;

import lombok.Data;

@Data
public class MultipartPresignPartResponse {
    private String sessionId;
    private Integer partNumber;
    private String presignedUrl;
    private Integer expiresInSeconds;
}
