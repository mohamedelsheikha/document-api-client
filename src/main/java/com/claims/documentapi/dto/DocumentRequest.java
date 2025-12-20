package com.claims.documentapi.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class DocumentRequest {
    private String name;
    private String documentClassId;
    private Map<String, Object> attributes = new HashMap<>();
}
