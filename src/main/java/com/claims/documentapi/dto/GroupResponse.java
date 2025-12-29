package com.claims.documentapi.dto;

import lombok.Data;

@Data
public class GroupResponse {
    private String id;
    private String name;
    private String description;
    private java.util.List<String> userIds;
    private java.util.List<String> userNames;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}
