package com.claims.documentapi.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String email;
    private String password;
    private String privilegeSetName; // Optional, will use default if not provided
}
