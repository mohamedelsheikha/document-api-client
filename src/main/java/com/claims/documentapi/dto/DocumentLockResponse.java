package com.claims.documentapi.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentLockResponse {
    private String documentId;
    private String lockedBy;
    private String lockId;
    private LocalDateTime lockExpiresAt;
}
