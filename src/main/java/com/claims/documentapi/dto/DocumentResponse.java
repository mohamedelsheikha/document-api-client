package com.claims.documentapi.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class DocumentResponse {
    private String id;
    private String documentClassId;
    private String documentClassName;
    private String accessControlListId;
    private Map<String, Object> attributes;
    private String createdBy;
    private String modifiedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<DocumentDto> documents;
}
