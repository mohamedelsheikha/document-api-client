package com.claims.documentapi;

import com.claims.documentapi.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client library for Document Management API
 * Provides type-safe API calls with built-in authentication and error handling
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentApiClient {
    
    private final RestTemplate restTemplate;
    private String baseUrl;
    /**
     * -- SETTER --
     *  Set authentication token for subsequent requests
     */
    @Setter
    private String authToken;

    private final Map<String, String> authTokensByTenant = new ConcurrentHashMap<>();

    @Getter
    @Setter
    private String activeTenant;

    @Setter
    @Getter
    private String defaultTenant = "claims";

    public static final String LOCK_HEADER = "X-Document-Lock-Id";
    
    public DocumentApiClient(String baseUrl) {
        this.restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
        this.baseUrl = baseUrl;
        this.activeTenant = defaultTenant;
    }

    /**
     * Clear authentication token
     */
    public void clearAuth() {
        this.authToken = null;
        if (activeTenant != null) {
            authTokensByTenant.remove(activeTenant);
        }
    }

    public void clearAuth(String tenant) {
        if (tenant == null || tenant.isBlank()) {
            return;
        }
        authTokensByTenant.remove(tenant);
        if (tenant.equals(activeTenant)) {
            this.authToken = null;
        }
    }

    public void clearAllAuth() {
        this.authToken = null;
        authTokensByTenant.clear();
    }

    public void setAuthToken(String tenant, String token) {
        if (tenant == null || tenant.isBlank()) {
            this.authToken = token;
            return;
        }
        if (token == null || token.isBlank()) {
            authTokensByTenant.remove(tenant);
        } else {
            authTokensByTenant.put(tenant, token);
        }
        if (tenant.equals(activeTenant)) {
            this.authToken = token;
        }
    }

    public void setActiveTenant(String tenant) {
        this.activeTenant = tenant;
        if (tenant != null && !tenant.isBlank()) {
            this.defaultTenant = tenant;
            this.authToken = authTokensByTenant.get(tenant);
        }
    }
    
    private HttpHeaders createHeaders() {
        return createHeaders(null);
    }

    private HttpHeaders createHeaders(String lockId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String effectiveToken = authToken;
        if ((effectiveToken == null || effectiveToken.isBlank()) && activeTenant != null && !activeTenant.isBlank()) {
            effectiveToken = authTokensByTenant.get(activeTenant);
        }
        if (effectiveToken != null && !effectiveToken.isBlank()) {
            headers.setBearerAuth(effectiveToken);
        }
        if (lockId != null && !lockId.isBlank()) {
            headers.set(LOCK_HEADER, lockId);
        }
        return headers;
    }
    
    private <T> ResponseEntity<T> exchange(String endpoint, HttpMethod method, Object body, ParameterizedTypeReference<T> responseType) {
        HttpEntity<?> entity = new HttpEntity<>(body, createHeaders());
        return restTemplate.exchange(baseUrl + endpoint, method, entity, responseType);
    }

    private <T> ResponseEntity<T> exchange(String endpoint,
                                          HttpMethod method,
                                          Object body,
                                          ParameterizedTypeReference<T> responseType,
                                          String lockId) {
        HttpEntity<?> entity = new HttpEntity<>(body, createHeaders(lockId));
        return restTemplate.exchange(baseUrl + endpoint, method, entity, responseType);
    }
    
    private <T> ResponseEntity<T> exchange(String endpoint, HttpMethod method, Object body, Class<T> responseType) {
        HttpEntity<?> entity = new HttpEntity<>(body, createHeaders());
        return restTemplate.exchange(baseUrl + endpoint, method, entity, responseType);
    }

    private <T> ResponseEntity<T> exchange(String endpoint,
                                          HttpMethod method,
                                          Object body,
                                          Class<T> responseType,
                                          String lockId) {
        HttpEntity<?> entity = new HttpEntity<>(body, createHeaders(lockId));
        return restTemplate.exchange(baseUrl + endpoint, method, entity, responseType);
    }
    
    // Authentication endpoints
    public LoginResponse login(LoginRequest request) {
        return login(defaultTenant, request);
    }

    public LoginResponse login(String tenant, LoginRequest request) {
        try {
            String endpoint = UriComponentsBuilder
                    .fromPath("/api/auth/login")
                    .queryParam("tenant", tenant)
                    .toUriString();

            ResponseEntity<LoginResponse> response = exchange(endpoint, HttpMethod.POST, request, LoginResponse.class);
            LoginResponse body = response.getBody();
            if (body != null && body.getToken() != null && !body.getToken().isBlank()) {
                setAuthToken(tenant, body.getToken());
                setActiveTenant(tenant);
            }
            return body;
        } catch (HttpClientErrorException e) {
            log.error("Login failed: {}", e.getResponseBodyAsString());
            throw e;
        }
    }
    
    public LoginResponse register(RegisterRequest request) {
        return register(defaultTenant, request);
    }

    public LoginResponse register(String tenant, RegisterRequest request) {
        try {
            String endpoint = UriComponentsBuilder
                    .fromPath("/api/auth/register")
                    .queryParam("tenant", tenant)
                    .toUriString();

            ResponseEntity<LoginResponse> response = exchange(endpoint, HttpMethod.POST, request, LoginResponse.class);
            LoginResponse body = response.getBody();
            if (body != null && body.getToken() != null && !body.getToken().isBlank()) {
                setAuthToken(tenant, body.getToken());
                setActiveTenant(tenant);
            }
            return body;
        } catch (HttpClientErrorException e) {
            log.error("Registration failed: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

    public List<TenantResponse> listTenants() {
        try {
            ResponseEntity<List<TenantResponse>> response = exchange("/api/admin/tenants", HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {
                    });
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to list tenants: {}", e.getResponseBodyAsString());
            throw e;
        }
    }
    
    // Document Class endpoints
    public List<DocumentClassResponse> getDocumentClasses() {
        try {
            ResponseEntity<List<DocumentClassResponse>> response = exchange("/api/admin/document-classes", HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {
                    });
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
                    new ParameterizedTypeReference<>() {
                    });
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to get users: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

    public UserResponse createUser(AdminCreateUserRequest request) {
        try {
            ResponseEntity<UserResponse> response = exchange("/api/admin/users", HttpMethod.POST, request, UserResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to create user: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

    public UserResponse getUserByUsername(String username) {
        try {
            ResponseEntity<UserResponse> response = exchange("/api/admin/users/username/" + username, HttpMethod.GET, null, UserResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to get user by username: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

    public void deleteUser(String userId) {
        try {
            exchange("/api/admin/users/" + userId, HttpMethod.DELETE, null, Void.class);
        } catch (HttpClientErrorException e) {
            log.error("Failed to delete user: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

    public UserResponse updateUserPrivilegeSet(String userId, String privilegeSetId) {
        try {
            String endpoint = UriComponentsBuilder
                    .fromPath("/api/admin/users/{userId}/privilege-set")
                    .queryParam("privilegeSetId", privilegeSetId)
                    .buildAndExpand(userId)
                    .toUriString();

            ResponseEntity<UserResponse> response = exchange(endpoint, HttpMethod.PATCH, null, UserResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to update user privilege set: {}", e.getResponseBodyAsString());
            throw e;
        }
    }
    
    // Group endpoints
    public List<GroupResponse> getGroups() {
        try {
            ResponseEntity<List<GroupResponse>> response = exchange("/api/admin/groups", HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {
                    });
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to get groups: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

    public GroupResponse createGroup(GroupRequest request) {
        try {
            ResponseEntity<GroupResponse> response = exchange("/api/admin/groups", HttpMethod.POST, request, GroupResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to create group: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

    public GroupResponse getGroupByName(String name) {
        try {
            ResponseEntity<GroupResponse> response = exchange("/api/admin/groups/name/" + name, HttpMethod.GET, null, GroupResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to get group by name: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

    public void deleteGroup(String groupId) {
        try {
            exchange("/api/admin/groups/" + groupId, HttpMethod.DELETE, null, Void.class);
        } catch (HttpClientErrorException e) {
            log.error("Failed to delete group: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

    public GroupResponse addUsersToGroup(String groupId, List<String> userIds) {
        try {
            ResponseEntity<GroupResponse> response = exchange("/api/admin/groups/" + groupId + "/users", HttpMethod.PATCH, userIds, GroupResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to add users to group: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

    // Privilege set endpoints
    public List<PrivilegeSetResponse> getPrivilegeSets() {
        try {
            ResponseEntity<List<PrivilegeSetResponse>> response = exchange("/api/admin/privilege-sets", HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {
                    });
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to get privilege sets: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

    public PrivilegeSetResponse getPrivilegeSetByName(String name) {
        try {
            ResponseEntity<PrivilegeSetResponse> response = exchange("/api/admin/privilege-sets/name/" + name, HttpMethod.GET, null, PrivilegeSetResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to get privilege set by name: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

    public PrivilegeSetResponse createPrivilegeSet(PrivilegeSetRequest request) {
        try {
            ResponseEntity<PrivilegeSetResponse> response = exchange("/api/admin/privilege-sets", HttpMethod.POST, request, PrivilegeSetResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to create privilege set: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

    public PrivilegeSetResponse updatePrivilegeSet(String privilegeSetId, PrivilegeSetRequest request) {
        try {
            ResponseEntity<PrivilegeSetResponse> response = exchange("/api/admin/privilege-sets/" + privilegeSetId, HttpMethod.PUT, request, PrivilegeSetResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to update privilege set: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

    public void deletePrivilegeSet(String privilegeSetId) {
        try {
            exchange("/api/admin/privilege-sets/" + privilegeSetId, HttpMethod.DELETE, null, Void.class);
        } catch (HttpClientErrorException e) {
            log.error("Failed to delete privilege set: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

    // ACL endpoints
    public List<AccessControlListResponse> getAcls() {
        try {
            ResponseEntity<List<AccessControlListResponse>> response = exchange("/api/admin/acls", HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {
                    });
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to get ACLs: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

    public AccessControlListResponse getAclByName(String name) {
        try {
            ResponseEntity<AccessControlListResponse> response = exchange("/api/admin/acls/name/" + name, HttpMethod.GET, null, AccessControlListResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to get ACL by name: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

    public AccessControlListResponse createAcl(AccessControlListRequest request) {
        try {
            ResponseEntity<AccessControlListResponse> response = exchange("/api/admin/acls", HttpMethod.POST, request, AccessControlListResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to create ACL: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

    public AccessControlListResponse updateAcl(String aclId, AccessControlListRequest request) {
        try {
            ResponseEntity<AccessControlListResponse> response = exchange("/api/admin/acls/" + aclId, HttpMethod.PUT, request, AccessControlListResponse.class);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to update ACL: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

    public void deleteAcl(String aclId) {
        try {
            exchange("/api/admin/acls/" + aclId, HttpMethod.DELETE, null, Void.class);
        } catch (HttpClientErrorException e) {
            log.error("Failed to delete ACL: {}", e.getResponseBodyAsString());
            throw e;
        }
    }
    
    // Privilege endpoints
    public List<PrivilegeResponse> getPrivileges() {
        try {
            ResponseEntity<List<PrivilegeResponse>> response = exchange("/api/admin/privileges", HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {
                    });
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
                    new ParameterizedTypeReference<>() {
                    });
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

    public DocumentLockResponse lockDocument(String id, Integer leaseSeconds) {
        try {
            DocumentLockRequest request = new DocumentLockRequest();
            request.setLeaseSeconds(leaseSeconds);
            ResponseEntity<DocumentLockResponse> response = exchange(
                    "/api/documents/" + id + "/lock",
                    HttpMethod.POST,
                    request,
                    DocumentLockResponse.class
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to lock document: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

    public DocumentLockResponse renewLock(String id, String lockId, Integer leaseSeconds) {
        try {
            DocumentLockRequest request = new DocumentLockRequest();
            request.setLeaseSeconds(leaseSeconds);
            ResponseEntity<DocumentLockResponse> response = exchange(
                    "/api/documents/" + id + "/lock/renew",
                    HttpMethod.POST,
                    request,
                    DocumentLockResponse.class,
                    lockId
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to renew lock: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

    public void unlockDocument(String id, String lockId) {
        try {
            exchange("/api/documents/" + id + "/unlock", HttpMethod.POST, null, Void.class, lockId);
        } catch (HttpClientErrorException e) {
            log.error("Failed to unlock document: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

    public List<DocumentAttachmentDto> uploadAttachments(String id, List<File> files) {
        return uploadAttachments(id, files, null);
    }

    public List<DocumentAttachmentDto> uploadAttachments(String id, List<File> files, String lockId) {
        try {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Document id cannot be null/blank");
            }
            if (files == null || files.isEmpty()) {
                throw new IllegalArgumentException("Files list cannot be null/empty");
            }

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            for (File file : files) {
                if (file == null) {
                    continue;
                }
                if (!file.exists() || !file.isFile()) {
                    throw new IllegalArgumentException("File does not exist or is not a regular file: " + file);
                }
                body.add("files", new FileSystemResource(file));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            if (authToken != null) {
                headers.setBearerAuth(authToken);
            }
            if (lockId != null && !lockId.isBlank()) {
                headers.set(LOCK_HEADER, lockId);
            }

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<List<DocumentAttachmentDto>> response = restTemplate.exchange(
                baseUrl + "/api/documents/" + id + "/attachments/batch",
                HttpMethod.POST,
                requestEntity,
                    new ParameterizedTypeReference<>() {
                    }
            );

            return response.getBody() != null ? response.getBody() : new ArrayList<>();
        } catch (Exception e) {
            log.error("Failed to upload attachments to document {}: {}", id, e.getMessage());
            throw new RuntimeException("Failed to upload attachments", e);
        }
    }

    public List<DocumentDto> getDocumentAttachments(String documentId) {
        try {
            ResponseEntity<List<DocumentDto>> response = exchange(
                "/api/documents/" + documentId + "/attachments", 
                HttpMethod.GET, 
                null,
                    new ParameterizedTypeReference<>() {
                    }
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to get document attachments: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

    public MultipartUploadInitResponse initMultipartUpload(String documentId, MultipartUploadInitRequest request, String lockId) {
        try {
            ResponseEntity<MultipartUploadInitResponse> response = exchange(
                    "/api/documents/" + documentId + "/attachments/multipart",
                    HttpMethod.POST,
                    request,
                    MultipartUploadInitResponse.class,
                    lockId
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to init multipart upload: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

    public MultipartUploadStatusResponse getMultipartUploadStatus(String documentId, String sessionId) {
        try {
            ResponseEntity<MultipartUploadStatusResponse> response = exchange(
                    "/api/documents/" + documentId + "/attachments/multipart/" + sessionId,
                    HttpMethod.GET,
                    null,
                    MultipartUploadStatusResponse.class
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to get multipart upload status: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

    public MultipartPresignPartResponse presignMultipartUploadPart(String documentId, String sessionId, int partNumber) {
        try {
            String endpoint = UriComponentsBuilder
                    .fromPath("/api/documents/{documentId}/attachments/multipart/{sessionId}/presign-part")
                    .queryParam("partNumber", partNumber)
                    .buildAndExpand(documentId, sessionId)
                    .toUriString();

            ResponseEntity<MultipartPresignPartResponse> response = exchange(
                    endpoint,
                    HttpMethod.POST,
                    null,
                    MultipartPresignPartResponse.class
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to presign multipart upload part: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

    public MultipartUploadCompleteResponse completeMultipartUpload(String documentId,
                                                                   String sessionId,
                                                                   MultipartUploadCompleteRequest request,
                                                                   String lockId) {
        try {
            ResponseEntity<MultipartUploadCompleteResponse> response = exchange(
                    "/api/documents/" + documentId + "/attachments/multipart/" + sessionId + "/complete",
                    HttpMethod.POST,
                    request,
                    MultipartUploadCompleteResponse.class,
                    lockId
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to complete multipart upload: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

    public void abortMultipartUpload(String documentId, String sessionId, String lockId) {
        try {
            exchange(
                    "/api/documents/" + documentId + "/attachments/multipart/" + sessionId + "/abort",
                    HttpMethod.POST,
                    null,
                    Void.class,
                    lockId
            );
        } catch (HttpClientErrorException e) {
            log.error("Failed to abort multipart upload: {}", e.getResponseBodyAsString());
            throw e;
        }
    }
    
    public String getAttachmentDownloadUrl(String documentId, String attachmentId) {
        try {
            ResponseEntity<String> response = exchange(
                "/api/documents/" + documentId + "/attachments/" + attachmentId + "/download", 
                HttpMethod.GET, 
                null, 
                String.class
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to get attachment download URL: {}", e.getResponseBodyAsString());
            throw e;
        }
    }
    
    public DocumentResponse updateDocument(String id, DocumentRequest request) {
        return updateDocument(id, request, null);
    }

    public DocumentResponse updateDocument(String id, DocumentRequest request, String lockId) {
        try {
            ResponseEntity<DocumentResponse> response = exchange("/api/documents/" + id, HttpMethod.PUT, request, DocumentResponse.class, lockId);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to update document: {}", e.getResponseBodyAsString());
            throw e;
        }
    }
    
    public void deleteDocument(String id) {
        deleteDocument(id, null);
    }

    public void deleteDocument(String id, String lockId) {
        try {
            exchange("/api/documents/" + id, HttpMethod.DELETE, null, Void.class, lockId);
        } catch (HttpClientErrorException e) {
            log.error("Failed to delete document: {}", e.getResponseBodyAsString());
            throw e;
        }
    }

    /**
     * Search for documents using the document search endpoint
     * @param searchRequest the search criteria including document class, filters, pagination, and sorting
     * @return list of documents matching the search criteria
     */
    public List<DocumentResponse> searchDocuments(DocumentSearchRequest searchRequest) {
        try {
            ResponseEntity<List<DocumentResponse>> response = exchange("/api/documents/search", HttpMethod.POST, searchRequest,
                    new ParameterizedTypeReference<>() {
                    });
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to search documents: {}", e.getResponseBodyAsString());
            throw e;
        }
    }
    
    /**
     * Search for documents with simplified parameters
     * @param documentClassId the document class ID to search within
     * @param attributeFilters optional map of attribute filters
     * @return list of documents matching the search criteria
     */
    public List<DocumentResponse> searchDocuments(String documentClassId, Map<String, Object> attributeFilters) {
        DocumentSearchRequest searchRequest = new DocumentSearchRequest();
        searchRequest.setDocumentClassId(documentClassId);
        searchRequest.setAttributeFilters(attributeFilters);
        return searchDocuments(searchRequest);
    }
    
    /**
     * Search for documents with pagination and sorting
     * @param documentClassId the document class ID to search within
     * @param attributeFilters optional map of attribute filters
     * @param page page number (0-based)
     * @param size page size
     * @param sortBy field to sort by
     * @param sortDirection sort direction (asc/desc)
     * @return list of documents matching the search criteria
     */
    public List<DocumentResponse> searchDocuments(String documentClassId, Map<String, Object> attributeFilters, 
                                                Integer page, Integer size, String sortBy, String sortDirection) {
        DocumentSearchRequest searchRequest = new DocumentSearchRequest();
        searchRequest.setDocumentClassId(documentClassId);
        searchRequest.setAttributeFilters(attributeFilters);
        searchRequest.setPage(page);
        searchRequest.setSize(size);
        searchRequest.setSortBy(sortBy);
        searchRequest.setSortDirection(sortDirection);
        return searchDocuments(searchRequest);
    }

    public String getBaseApiUrl() {
        return baseUrl;
    }
}
