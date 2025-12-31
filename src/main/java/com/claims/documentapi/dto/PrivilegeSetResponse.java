package com.claims.documentapi.dto;

import lombok.Data;

import java.util.List;

@Data
public class PrivilegeSetResponse {
    private String id;
    private String name;
    private String description;
    private List<String> privilegeIds;
}
