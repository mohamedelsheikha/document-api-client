package com.claims.documentapi.dto;

import lombok.Data;

import java.util.Map;

@Data
public class AccessControlListResponse {

    private String id;
    private String name;
    private String description;

    private Map<String, String> association;
}
