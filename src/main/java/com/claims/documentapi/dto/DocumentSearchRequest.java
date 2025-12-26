package com.claims.documentapi.dto;

import lombok.Data;

import java.util.Map;

@Data
public class DocumentSearchRequest {
    
    private String documentClassId;
    
    private Map<String, Object> attributeFilters;
    
    // Optional pagination parameters
    private Integer page = 0;
    private Integer size = 50;
    
    // Optional sorting
    private String sortBy = "createdAt";
    private String sortDirection = "desc";
}
