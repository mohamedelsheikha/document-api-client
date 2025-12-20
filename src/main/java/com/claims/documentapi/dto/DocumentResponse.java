package com.claims.documentapi.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class DocumentResponse {
    private String id;
    private String documentClassId;
    private String documentClassName;
    private Map<String, Object> attributes;
    private String createdBy;
    private String modifiedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
