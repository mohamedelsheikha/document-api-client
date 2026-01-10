package com.claims.documentapi.dto;

import lombok.Data;

@Data
public class MultipartCompletedPart {
    private Integer partNumber;
    private String eTag;
}
