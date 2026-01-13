package com.claims.documentapi.dto;

import lombok.Data;

@Data
public class TenantResponse {
    private String tenantKey;
    private boolean enabled;
    private String mongoSecretId;
}
