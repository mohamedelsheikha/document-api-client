package com.claims.documentapi.dto;

import lombok.Data;

@Data
public class DocumentLockRequest {
    private Integer leaseSeconds;
}
