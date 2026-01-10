package com.claims.documentapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MultipartCompletedPart {
    private Integer partNumber;
    @JsonProperty("eTag")
    private String eTag;
}
