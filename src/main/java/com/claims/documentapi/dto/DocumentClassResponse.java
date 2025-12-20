package com.claims.documentapi.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class DocumentClassResponse {
    private String id;
    private String name;
    private String displayName;
    private String description;
    private List<AttributeDefinition> attributes;
    private String aclId;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    public static class AttributeDefinition {
        private String name;
        private String displayName;
        private String type;
        private Integer length;
        private boolean multiValue = false;
        private boolean required;
        private boolean indexed;
        private String validationRules;
    }
}
