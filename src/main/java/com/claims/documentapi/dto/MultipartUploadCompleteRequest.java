package com.claims.documentapi.dto;

import lombok.Data;

import java.util.List;

@Data
public class MultipartUploadCompleteRequest {
    private List<MultipartCompletedPart> parts;
}
