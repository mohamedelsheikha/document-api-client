package com.claims.documentapi.dto;

import lombok.Data;

@Data
public class GroupResponse {
    private String id;
    private String name;
    private String description;
    private String privilegeSetId;
    private String createdBy;
    private String createdAt;
}
