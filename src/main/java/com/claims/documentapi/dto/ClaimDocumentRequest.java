package com.claims.documentapi.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ClaimDocumentRequest {

    private String claimNumber;

    private List<String> claimantNames;

    private LocalDate dateOfLoss;

    private String description;
}
