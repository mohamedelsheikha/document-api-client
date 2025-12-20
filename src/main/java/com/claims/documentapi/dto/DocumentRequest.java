package com.claims.documentapi.dto;

import lombok.Data;
import java.util.List;

@Data
public class DocumentRequest {
    private String name;
    private String documentClassId;
    private List<DocumentAttribute> attributes;
    
    @Data
    public static class DocumentAttribute {
        private String name;
        private Object value;
    }
}
