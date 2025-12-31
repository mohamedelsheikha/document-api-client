package com.claims.documentapi.dto;

import lombok.Data;

import java.util.List;

@Data
public class GroupRequest {
    private String name;
    private String description;
    private List<String> userIds;
}
