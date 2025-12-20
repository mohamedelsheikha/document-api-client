package com.claims.documentapi.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DocumentResponse {
    private String id;
    private String name;
    private String documentClassId;
    private String documentClassName;
    private List<DocumentAttribute> attributes;
    private String status;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    public static class DocumentAttribute {
        private String name;
        private String displayName;
        private String type;
        private Object value;
    }
}
