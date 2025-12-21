package com.claims.documentapi;

import com.claims.documentapi.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

/**
 * Client library for Document Management API
 * Provides type-safe API calls with built-in authentication and error handling
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentApiClient {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private String baseUrl;
    private String authToken;
    
    public DocumentApiClient(String baseUrl) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.baseUrl = baseUrl;
    }

    /**
     * Set authentication token for subsequent requests
     */
    public void setAuthToken(String token) {
        this.authToken = token;
    }

    /**
     * Clear authentication token
     */
    public void clearAuth() {
        this.authToken = null;
    }
    
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (authToken != null) {
            headers.setBearerAuth(authToken);
        }
        return headers;
    }
    
    private <T> ResponseEntity<T> exchange(String endpoint, HttpMethod method, Object body, ParameterizedTypeReference<T> responseType) {
        HttpEntity<?> entity = new HttpEntity<>(body, createHeaders());
        return restTemplate.exchange(baseUrl + endpoint, method, entity, responseType);
    }
    
    private <T> ResponseEntity<T> exchange(String endpoint, HttpMethod method, Object body, Class<T> responseType) {
        HttpEntity<?> entity = new HttpEntity<>(body, createHeaders());
        return restTemplate.exchange(baseUrl + endpoint, method, entity, responseType);
    }
    
    // Authentication endpoints
    public LoginResponse login(LoginRequest request) {
        try {
            ResponseEntity<LoginResponse> response = exchange("/api/auth/login", HttpMethod.POST, request, LoginResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Login failed: {}", e.getResponseBodyAsString());
            throw e;
        }
    }
    
    public LoginResponse register(RegisterRequest request) {
        try {
            ResponseEntity<LoginResponse> response = exchange("/api/auth/register", HttpMethod.POST, request, LoginResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Registration failed: {}", e.getResponseBodyAsString());
            throw e;
        }
    }
    
    // Document Class endpoints
    public List<DocumentClassResponse> getDocumentClasses() {
        try {
            ResponseEntity<List<DocumentClassResponse>> response = exchange("/api/admin/document-classes", HttpMethod.GET, null, 
                new ParameterizedTypeReference<List<DocumentClassResponse>>() {});
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to get document classes: {}", e.getResponseBodyAsString());
            throw e;
        }
    }
    
    public DocumentClassResponse getDocumentClassById(String id) {
        try {
            ResponseEntity<DocumentClassResponse> response = exchange("/api/admin/document-classes/" + id, HttpMethod.GET, null, DocumentClassResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to get document class: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

    public DocumentClassResponse getDocumentClassByName(String name) {
        try {
            ResponseEntity<DocumentClassResponse> response = exchange("/api/admin/document-classes/name/" + name, HttpMethod.GET, null, DocumentClassResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to get document class: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

    public DocumentClassResponse createDocumentClass(DocumentClassRequest request) {
        try {
            ResponseEntity<DocumentClassResponse> response = exchange("/api/admin/document-classes", HttpMethod.POST, request, DocumentClassResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to create document class: {}", e.getResponseBodyAsString());
            throw e;
        }
    }
    
    public DocumentClassResponse updateDocumentClass(String id, DocumentClassRequest request) {
        try {
            ResponseEntity<DocumentClassResponse> response = exchange("/api/admin/document-classes/" + id, HttpMethod.PUT, request, DocumentClassResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to update document class: {}", e.getResponseBodyAsString());
            throw e;
        }
    }
    
    public void deleteDocumentClass(String id) {
        try {
            exchange("/api/admin/document-classes/" + id, HttpMethod.DELETE, null, Void.class);
        } catch (HttpClientErrorException e) {
            log.error("Failed to delete document class: {}", e.getResponseBodyAsString());
            throw e;
        }
    }
    
    // User endpoints
    public List<UserResponse> getUsers() {
        try {
            ResponseEntity<List<UserResponse>> response = exchange("/api/admin/users", HttpMethod.GET, null, 
                new ParameterizedTypeReference<List<UserResponse>>() {});
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to get users: {}", e.getResponseBodyAsString());
            throw e;
        }
    }
    
    // Group endpoints
    public List<GroupResponse> getGroups() {
        try {
            ResponseEntity<List<GroupResponse>> response = exchange("/api/admin/groups", HttpMethod.GET, null, 
                new ParameterizedTypeReference<List<GroupResponse>>() {});
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to get groups: {}", e.getResponseBodyAsString());
            throw e;
        }
    }
    
    // Privilege endpoints
    public List<PrivilegeResponse> getPrivileges() {
        try {
            ResponseEntity<List<PrivilegeResponse>> response = exchange("/api/admin/privileges", HttpMethod.GET, null, 
                new ParameterizedTypeReference<List<PrivilegeResponse>>() {});
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to get privileges: {}", e.getResponseBodyAsString());
            throw e;
        }
    }
    
    public PrivilegeResponse createPrivilege(PrivilegeRequest request) {
        try {
            ResponseEntity<PrivilegeResponse> response = exchange("/api/admin/privileges", HttpMethod.POST, request, PrivilegeResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to create privilege: {}", e.getResponseBodyAsString());
            throw e;
        }
    }
    
    public PrivilegeResponse updatePrivilege(String id, PrivilegeRequest request) {
        try {
            ResponseEntity<PrivilegeResponse> response = exchange("/api/admin/privileges/" + id, HttpMethod.PUT, request, PrivilegeResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to update privilege: {}", e.getResponseBodyAsString());
            throw e;
        }
    }
    
    public void deletePrivilege(String id) {
        try {
            exchange("/api/admin/privileges/" + id, HttpMethod.DELETE, null, Void.class);
        } catch (HttpClientErrorException e) {
            log.error("Failed to delete privilege: {}", e.getResponseBodyAsString());
            throw e;
        }
    }
    
    // Document endpoints
    public List<DocumentResponse> getDocuments() {
        try {
            ResponseEntity<List<DocumentResponse>> response = exchange("/api/documents", HttpMethod.GET, null, 
                new ParameterizedTypeReference<List<DocumentResponse>>() {});
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to get documents: {}", e.getResponseBodyAsString());
            throw e;
        }
    }
    
    public DocumentResponse getDocument(String id) {
        try {
            ResponseEntity<DocumentResponse> response = exchange("/api/documents/" + id, HttpMethod.GET, null, DocumentResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to get document: {}", e.getResponseBodyAsString());
            throw e;
        }
    }
    
    public DocumentResponse createDocument(DocumentRequest request) {
        try {
            ResponseEntity<DocumentResponse> response = exchange("/api/documents", HttpMethod.POST, request, DocumentResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to create document: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

    public UploadResponse uploadAttachmentToDocumentId(String id, File file) {
        try {
            // Create multipart request body
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            // Add file as ByteArrayResource with null safety
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            body.add("file", new ByteArrayResource(fileBytes) {
                @Override
                public String getFilename() {
                    return file.getName();
                }
            });
            
            // Create authenticated headers for multipart request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            if (authToken != null) {
                headers.setBearerAuth(authToken);
            }
            
            // Create request entity
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            // Use RestTemplate exchange for multipart upload with authentication
            ResponseEntity<UploadResponse> response = restTemplate.exchange(
                baseUrl + "/api/documents/" + id + "/attachments",
                HttpMethod.POST,
                requestEntity,
                UploadResponse.class
            );
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to upload attachment to document {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to upload attachment", e);
        }
    }

    public UploadResponse uploadMultipleAttachments(String id, List<File> files) {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // Add multiple files
            for (File file : files) {
                body.add("files", new ByteArrayResource(Files.readAllBytes(file.toPath())) {
                    @Override
                    public String getFilename() {
                        return file.getName();
                    }
                });
            }

            // Add metadata if needed
            body.add("documentId", id);
            body.add("uploadedBy", "user123");

            // Create authenticated headers for multipart request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            if (authToken != null) {
                headers.setBearerAuth(authToken);
            }

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<UploadResponse> response = restTemplate.exchange(
                    baseUrl + "/api/documents/" + id + "/attachments/batch",
                    HttpMethod.POST,
                    requestEntity,
                    UploadResponse.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to upload multiple attachments: {}", e.getMessage());
            throw new RuntimeException("Failed to upload attachments", e);
        }
    }
    
    public DocumentResponse updateDocument(String id, DocumentRequest request) {
        try {
            ResponseEntity<DocumentResponse> response = exchange("/api/documents/" + id, HttpMethod.PUT, request, DocumentResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to update document: {}", e.getResponseBodyAsString());
            throw e;
        }
    }
    
    public void deleteDocument(String id) {
        try {
            exchange("/api/documents/" + id, HttpMethod.DELETE, null, Void.class);
        } catch (HttpClientErrorException e) {
            log.error("Failed to delete document: {}", e.getResponseBodyAsString());
            throw e;
        }
    }
    
    // Document Upload Methods
    
    /**
     * Upload a claim document with files (multipart/form-data)
     */
    public ClaimDocumentResponse uploadDocument(ClaimDocumentRequest request, java.io.File file, java.io.File... additionalFiles) {
        try {
            java.util.List<org.springframework.web.multipart.MultipartFile> files = new java.util.ArrayList<>();
            
            // Convert File to MultipartFile
            if (file != null && file.exists()) {
                files.add(convertToMultipartFile(file));
            }
            for (java.io.File additionalFile : additionalFiles) {
                if (additionalFile != null && additionalFile.exists()) {
                    files.add(convertToMultipartFile(additionalFile));
                }
            }
            
            return uploadDocumentWithFiles(request, files);
        } catch (Exception e) {
            log.error("Failed to upload document with files: {}", e.getMessage());
            throw new RuntimeException("Failed to upload document with files", e);
        }
    }
    
    /**
     * Upload a claim document from URL
     */
    public ClaimDocumentResponse uploadDocumentFromUrl(ClaimDocumentRequest request, String documentUrl) {
        try {
            // Create request with URL
            ClaimDocumentRequest urlRequest = new ClaimDocumentRequest();
            urlRequest.setClaimNumber(request.getClaimNumber());
            urlRequest.setClaimantNames(request.getClaimantNames());
            urlRequest.setDateOfLoss(request.getDateOfLoss());
            urlRequest.setDescription(request.getDescription());
            
            // Add URL to request (you may need to modify the DTO to support this)
            // For now, we'll send the URL in the description or create a new field
            
            ResponseEntity<ClaimDocumentResponse> response = exchange("/api/documents", HttpMethod.POST, urlRequest, ClaimDocumentResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to upload document from URL: {}", e.getResponseBodyAsString());
            throw e;
        }
    }
    
    /**
     * Upload a claim document with multiple files (multipart/form-data)
     */
    public ClaimDocumentResponse uploadDocumentWithFiles(ClaimDocumentRequest request, List<MultipartFile> files) {
        try {
            // Create multipart request
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("request", request);
            
            // Add files to request
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    body.add("files", new ByteArrayResource(file.getBytes()) {
                        @Override
                        public String getFilename() {
                            return file.getOriginalFilename();
                        }
                    });
                }
            }
            
            // Set headers for multipart request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            // Create request entity
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            // Make the request
            ResponseEntity<ClaimDocumentResponse> response = restTemplate.postForEntity(
                baseUrl + "/api/claims", 
                requestEntity, 
                ClaimDocumentResponse.class
            );
            
            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to upload document with files: {}", e.getMessage());
            throw new RuntimeException("Failed to upload document with files", e);
        }
    }
    
    /**
     * Convert File to MultipartFile
     */
    private MultipartFile convertToMultipartFile(File file) {
        try {
            return new MultipartFile() {
                @Override
                public String getName() {
                    return "file";
                }
                
                @Override
                public String getOriginalFilename() {
                    return file.getName();
                }
                
                @Override
                public String getContentType() {
                    return "application/octet-stream";
                }
                
                @Override
                public boolean isEmpty() {
                    return file.length() == 0;
                }
                
                @Override
                public long getSize() {
                    return file.length();
                }
                
                @Override
                public byte[] getBytes() throws IOException {
                    return Files.readAllBytes(file.toPath());
                }
                
                @Override
                public InputStream getInputStream() throws IOException {
                    return new FileInputStream(file);
                }
                
                @Override
                public void transferTo(File dest) throws IOException, IllegalStateException {
                    if (dest == null) {
                        throw new IllegalArgumentException("Destination file cannot be null");
                    }
                    Files.copy(file.toPath(), dest.toPath());
                }
            };
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert file to MultipartFile", e);
        }
    }
}
