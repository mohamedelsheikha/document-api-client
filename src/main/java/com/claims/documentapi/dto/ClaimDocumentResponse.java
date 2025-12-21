package com.claims.documentapi.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ClaimDocumentResponse {
    private String id;
    private String claimNumber;
    private List<String> claimantNames;
    private LocalDate dateOfLoss;
    private String description;
    private List<DocumentDto> documents;
    private String createdBy;
    private String modifiedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
