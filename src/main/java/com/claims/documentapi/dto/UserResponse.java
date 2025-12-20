package com.claims.documentapi.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserResponse {
    private String id;
    private String username;
    private String email;
    private String privilegeSetId;
    private String privilegeSetName;
    private List<String> groupIds;
    private boolean enabled;
    private boolean accountNonExpired;
    private boolean accountNonLocked;
    private boolean credentialsNonExpired;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getRole() {
        return privilegeSetName; // Map privilege set name to role for compatibility
    }

}
