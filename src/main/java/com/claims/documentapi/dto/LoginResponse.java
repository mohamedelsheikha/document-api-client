package com.claims.documentapi.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class LoginResponse {
    private String token;
    private String tokenType;
    private String userId;
    private String username;
    private String privilegeSetId;
    private String privilegeSetName;
    
    // Add getters/setters for backward compatibility with existing code that expects different field names
    public String getRefreshToken() {
        return null; // Not supported in current implementation
    }
    
    public Long getExpiresIn() {
        return null; // Not supported in current implementation
    }
    
    public String getRole() {
        return privilegeSetName; // Map privilege set name to role for compatibility
    }
}
