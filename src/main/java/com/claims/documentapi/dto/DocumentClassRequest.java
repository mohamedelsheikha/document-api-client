package com.claims.documentapi.dto;

import lombok.Data;
import java.util.List;

@Data
public class DocumentClassRequest {
    private String name;
    private String displayName;
    private String description;
    private String accessControlListId;
    private boolean classLevelAcl = false; // Default to per-document ACL
    private List<AttributeDefinition> attributes;

    @Data
    public static class AttributeDefinition {
        private String name;
        private String displayName;
        private String type;
        private Integer length;
        private boolean multiValue = false;
        private boolean required;
        private boolean indexed;
        private String validationPattern;
        private String description;
        private String defaultValue;

        public enum AttributeType {
            STRING,
            NUMBER,
            DATE,
            BOOLEAN,
            TEXT, // Long text
            EMAIL,
            PHONE,
            CURRENCY,
            PERCENTAGE,
            URL,
            REFERENCE // Reference to another document
        }

    }


}
