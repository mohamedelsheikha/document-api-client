package com.claims.documentapi;

import com.claims.documentapi.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

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
    
    public DocumentClassResponse getDocumentClass(String id) {
        try {
            ResponseEntity<DocumentClassResponse> response = exchange("/api/admin/document-classes/" + id, HttpMethod.GET, null, DocumentClassResponse.class);
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
    
    public DocumentResponse createDocument(DocumentRequest request) {
        try {
            ResponseEntity<DocumentResponse> response = exchange("/api/documents", HttpMethod.POST, request, DocumentResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to create document: {}", e.getResponseBodyAsString());
            throw e;
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
}
