package com.claims.documentapi.dto;

import lombok.Data;

@Data
public class AdminCreateUserRequest {
    private String username;
    private String email;
    private String password;
    private String privilegeSetId;
}
