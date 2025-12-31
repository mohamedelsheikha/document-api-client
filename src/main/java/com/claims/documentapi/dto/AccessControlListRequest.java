package com.claims.documentapi.dto;

import lombok.Data;

import java.util.Map;

@Data
public class AccessControlListRequest {

    private String name;
    private String description;

    private Map<String, String> association;
}
