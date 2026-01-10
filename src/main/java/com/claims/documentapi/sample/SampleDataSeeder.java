package com.claims.documentapi.sample;

import com.claims.documentapi.DocumentApiClient;
import com.claims.documentapi.dto.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.*;

public class SampleDataSeeder {

    private static final String PREFIX = "SAMPLE_";
    private static final String DEFAULT_USER_PASSWORD = "UserPass123!";

    private final DocumentApiClient client;

    public SampleDataSeeder(String baseUrl) {
        this.client = new DocumentApiClient(baseUrl);
    }

    public static void main(String[] args) {
        String baseUrl = args.length > 0 ? args[0] : "http://localhost:8080";
        String adminUsername = args.length > 1 ? args[1] : "S_admin";
        String adminPassword = args.length > 2 ? args[2] : "S_AdminPass123!";

        // Optional file paths for attachment upload
        List<String> filePaths = args.length > 3
            ? Arrays.asList(Arrays.copyOfRange(args, 3, args.length))
            : List.of(
                "/home/mohamed/Documents/SampleDocs/sample.pdf",
                "/home/mohamed/Documents/SampleDocs/sample.txt"
            );

        SampleDataSeeder seeder = new SampleDataSeeder(baseUrl);
        seeder.run(adminUsername, adminPassword, filePaths);
    }

    public void run(String adminUsername, String adminPassword, List<String> filePaths) {
        loginAsAdmin(adminUsername, adminPassword);

        cleanupSampleData();

        PrivilegeSetResponse readOnly = ensurePrivilegeSet(
            PREFIX + "ReadOnly",
            "Seeded read-only privilege set (search + download)",
            List.of("LOGIN", "SEARCH_CLAIMS", "READ_CLAIM", "DOWNLOAD_DOCUMENT")
        );
        System.out.println("Created PrivilegeSet: " + readOnly.getName());


        PrivilegeSetResponse readWrite = ensurePrivilegeSet(
            PREFIX + "ReadWrite",
            "Seeded read-write privilege set (create + upload + search + download)",
            List.of("LOGIN", "SEARCH_CLAIMS", "READ_CLAIM", "CREATE_DOCUMENT", "UPDATE_CLAIM", "UPLOAD_DOCUMENT", "DOWNLOAD_DOCUMENT")
        );
        System.out.println("Created PrivilegeSet: " + readWrite.getName());

        // Create 4 users (via /api/auth/register)
        List<LoginResponse> users = new ArrayList<>();
        List<SeededUser> seededUsers = new ArrayList<>();
        users.add(registerUser(PREFIX + "user1", PREFIX.toLowerCase() + "user1@example.com", DEFAULT_USER_PASSWORD, readWrite.getName()));
        seededUsers.add(new SeededUser(PREFIX + "user1", DEFAULT_USER_PASSWORD));
        users.add(registerUser(PREFIX + "user2", PREFIX.toLowerCase() + "user2@example.com", DEFAULT_USER_PASSWORD, readWrite.getName()));
        seededUsers.add(new SeededUser(PREFIX + "user2", DEFAULT_USER_PASSWORD));
        users.add(registerUser(PREFIX + "user3", PREFIX.toLowerCase() + "user3@example.com", DEFAULT_USER_PASSWORD, readWrite.getName()));
        seededUsers.add(new SeededUser(PREFIX + "user3", DEFAULT_USER_PASSWORD));
        users.add(registerUser(PREFIX + "user4", PREFIX.toLowerCase() + "user4@example.com", DEFAULT_USER_PASSWORD, readWrite.getName()));
        seededUsers.add(new SeededUser(PREFIX + "user4", DEFAULT_USER_PASSWORD));

        List<String> userIds = users.stream().map(LoginResponse::getUserId).toList();

        // Create 2 groups
        GroupResponse groupA = ensureGroup(PREFIX + "GroupA", "Seeded group A", List.of(userIds.get(0), userIds.get(2)));
        System.out.println("Create group: " + groupA.getName());
        GroupResponse groupB = ensureGroup(PREFIX + "GroupB", "Seeded group B", List.of(userIds.get(1), userIds.get(3)));
        System.out.println("Create group: " + groupB.getName());

        // Create 2 ACLs
        AccessControlListResponse aclA = ensureAcl(
            PREFIX + "ACL_A",
            "Seeded ACL A",
            Map.of(groupA.getName(), readWrite.getId())
        );

        AccessControlListResponse aclB = ensureAcl(
            PREFIX + "ACL_B",
            "Seeded ACL B",
            Map.of(groupB.getName(), readWrite.getId())
        );

        AccessControlListResponse aclClassLevel = ensureAcl(
            PREFIX + "ACL_CLASSLEVEL",
            "Seeded ACL for class-level document classes (GroupA + GroupB)",
            new HashMap<>() {{
                put(groupA.getName(), readWrite.getId());
                put(groupB.getName(), readWrite.getId());
            }}
        );

        // Create 2 sample document classes
        DocumentClassResponse classA = ensureDocumentClass(
            PREFIX + "ClassA",
            "Sample Class A",
            "Seeded class A",
            aclClassLevel.getId(),
            true,
            List.of(requiredStringAttr("title", "Title"), requiredStringAttr("category", "Category"))
        );

        DocumentClassResponse classB = ensureDocumentClass(
            PREFIX + "ClassB",
            "Sample Class B",
            "Seeded class B",
            null,
            false,
            List.of(requiredStringAttr("title", "Title"), requiredStringAttr("department", "Department"))
        );

        // Create documents (ClassA = class-level ACL; ClassB = per-document ACL)
        DocumentResponse doc1 = createSampleDocument(classA.getId(), aclA.getId(), Map.of("title", PREFIX + "Doc1", "category", "A"));
        System.out.println("Created ClassA document (class-level ACL applied): " + doc1.getId() + " acl=" + doc1.getAccessControlListId());

        DocumentResponse doc2 = createSampleDocument(classA.getId(), aclB.getId(), Map.of("title", PREFIX + "Doc2", "category", "B"));
        System.out.println("Created ClassA document (class-level ACL applied): " + doc2.getId() + " acl=" + doc2.getAccessControlListId());

        DocumentResponse doc3 = createSampleDocument(classB.getId(), aclA.getId(), Map.of("title", PREFIX + "Doc3", "department", "HR"));
        System.out.println("Created ClassB document (per-document ACL): " + doc3.getId() + " acl=" + doc3.getAccessControlListId());

        // Upload attachments (best-effort; skip missing files)
        uploadAttachmentsBestEffort(doc1.getId(), filePaths);
        uploadAttachmentsBestEffort(doc2.getId(), filePaths);

        // Demo: update (lock -> update -> unlock)
        System.out.println("\n=== Demo: Update (with lease lock) ===");
        DocumentLockResponse lock = client.lockDocument(doc1.getId(), 900);
        try {
            DocumentRequest updateRequest = new DocumentRequest();
            updateRequest.setDocumentClassId(doc1.getDocumentClassId());
            updateRequest.setAccessControlListId(doc1.getAccessControlListId());
            updateRequest.getAttributes().putAll(doc1.getAttributes());
            updateRequest.getAttributes().put("category", "UPDATED");
            updateRequest.getAttributes().put("title", PREFIX + "Doc1_UPDATED");

            DocumentResponse updated = client.updateDocument(doc1.getId(), updateRequest, lock.getLockId());
            System.out.println("Updated document: " + updated.getId() +
                    " title=" + updated.getAttributes().get("title") +
                    " category=" + updated.getAttributes().get("category"));
        } finally {
            safeRun(() -> client.unlockDocument(doc1.getId(), lock.getLockId()));
        }

        // Demo: search
        System.out.println("\n=== Demo: Search ===");
        List<DocumentResponse> searchResults = client.searchDocuments(
            classA.getId(),
            Map.of("title", PREFIX + "Doc")
        );
        System.out.println("Search results (ClassA, title contains '" + PREFIX + "Doc'): " + searchResults.size());
        for (DocumentResponse d : searchResults) {
            System.out.println("- " + d.getId() + " title=" + d.getAttributes().get("title") + " acl=" + d.getAccessControlListId());
        }

        // Demo: download attachment presigned URL
        System.out.println("\n=== Demo: Download Attachment URL ===");
        List<DocumentDto> attachments = client.getDocumentAttachments(doc1.getId());
        if (attachments != null && !attachments.isEmpty()) {
            DocumentDto first = attachments.get(0);
            String url = client.getAttachmentDownloadUrl(doc1.getId(), first.getId());
            System.out.println("Download URL for attachment id=" + first.getId() + ":\n" + url);
        } else {
            System.out.println("No attachments found for doc1=" + doc1.getId() + ".");
        }

        System.out.println("\n=== Demo: Per-user document operations ===");
        client.clearAuth();
        for (SeededUser user : seededUsers) {
            loginAsUser(user.username(), user.password());

            String userAclId = (user.username().endsWith("user1") || user.username().endsWith("user3"))
                ? aclA.getId()
                : aclB.getId();

            try {
                DocumentResponse createdClassLevel = createSampleDocument(
                    classA.getId(),
                    userAclId,
                    Map.of("title", PREFIX + user.username() + "_ClassLevel_Doc", "category", "USER")
                );
                System.out.println("User " + user.username() + " created ClassA doc: " + createdClassLevel.getId() + " acl=" + createdClassLevel.getAccessControlListId());

                DocumentResponse createdPerDoc = createSampleDocument(
                    classB.getId(),
                    userAclId,
                    Map.of("title", PREFIX + user.username() + "_PerDoc_Doc", "department", "USER")
                );
                System.out.println("User " + user.username() + " created ClassB doc: " + createdPerDoc.getId() + " acl=" + createdPerDoc.getAccessControlListId());

                List<DocumentResponse> visibleDocs = client.getDocuments();
                System.out.println("User " + user.username() + " can see documents: " + (visibleDocs != null ? visibleDocs.size() : 0));

                DocumentResponse viewed = client.getDocument(createdPerDoc.getId());
                System.out.println("User " + user.username() + " viewed document: " + viewed.getId() + " title=" + viewed.getAttributes().get("title"));

                DocumentLockResponse userLock = client.lockDocument(createdPerDoc.getId(), 900);
                try {
                    DocumentRequest updateRequest = new DocumentRequest();
                    updateRequest.setDocumentClassId(createdPerDoc.getDocumentClassId());
                    updateRequest.setAccessControlListId(createdPerDoc.getAccessControlListId());
                    updateRequest.getAttributes().putAll(createdPerDoc.getAttributes());
                    updateRequest.getAttributes().put("title", PREFIX + user.username() + "_PerDoc_Doc_UPDATED");

                    DocumentResponse updated = client.updateDocument(createdPerDoc.getId(), updateRequest, userLock.getLockId());
                    System.out.println("User " + user.username() + " updated document: " + updated.getId() + " title=" + updated.getAttributes().get("title"));
                } finally {
                    safeRun(() -> client.unlockDocument(createdPerDoc.getId(), userLock.getLockId()));
                }
            } catch (Exception e) {
                System.out.println("User " + user.username() + " document flow failed: " + e.getMessage());
            } finally {
                client.clearAuth();
            }
        }

        System.out.println("\n=== Sample data seeding completed ===");
    }

    private void loginAsAdmin(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);

        LoginResponse response = client.login(request);
        client.setAuthToken(response.getToken());
        System.out.println("Logged in as admin: " + response.getUsername());
    }

    private void loginAsUser(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);

        LoginResponse response = client.login(request);
        client.setAuthToken(response.getToken());
        System.out.println("Logged in as user: " + response.getUsername());
    }

    private void cleanupSampleData() {
        System.out.println("\n=== Cleanup (prefix: " + PREFIX + ") ===");

        // Documents (delete by seeded attribute)
        List<DocumentResponse> documents = safeList(client::getDocuments);
        if (documents != null) {
            for (DocumentResponse d : documents) {
                Object title = d.getAttributes() != null ? d.getAttributes().get("title") : null;
                if (title instanceof String && ((String) title).startsWith(PREFIX)) {
                    safeRun(() -> {
                        DocumentLockResponse lock = client.lockDocument(d.getId(), 300);
                        try {
                            client.deleteDocument(d.getId(), lock.getLockId());
                        } finally {
                            safeRun(() -> client.unlockDocument(d.getId(), lock.getLockId()));
                        }
                    });
                    System.out.println("Deleted document: " + d.getId());
                }
            }
        }

        // Document classes
        List<DocumentClassResponse> classes = safeList(client::getDocumentClasses);
        if (classes != null) {
            for (DocumentClassResponse dc : classes) {
                if (dc.getName() != null && dc.getName().startsWith(PREFIX)) {
                    safeRun(() -> client.deleteDocumentClass(dc.getId()));
                    System.out.println("Deleted document class: " + dc.getName());
                }
            }
        }

        // ACLs
        List<AccessControlListResponse> acls = safeList(client::getAcls);
        if (acls != null) {
            for (AccessControlListResponse acl : acls) {
                if (acl.getName() != null && acl.getName().startsWith(PREFIX)) {
                    safeRun(() -> client.deleteAcl(acl.getId()));
                    System.out.println("Deleted ACL: " + acl.getName());
                }
            }
        }

        // Groups
        List<GroupResponse> groups = safeList(client::getGroups);
        if (groups != null) {
            for (GroupResponse group : groups) {
                if (group.getName() != null && group.getName().startsWith(PREFIX)) {
                    safeRun(() -> client.deleteGroup(group.getId()));
                    System.out.println("Deleted group: " + group.getName());
                }
            }
        }

        // Users
        for (int i = 1; i <= 4; i++) {
            String username = PREFIX + "user" + i;
            try {
                UserResponse user = client.getUserByUsername(username);
                if (user != null && user.getId() != null) {
                    safeRun(() -> client.deleteUser(user.getId()));
                    System.out.println("Deleted user: " + username);
                }
            } catch (Exception ignored) {
                // user not found
            }
        }

        // Privilege sets
        List<PrivilegeSetResponse> privilegeSets = safeList(client::getPrivilegeSets);
        if (privilegeSets != null) {
            for (PrivilegeSetResponse ps : privilegeSets) {
                if (ps.getName() != null && ps.getName().startsWith(PREFIX)) {
                    safeRun(() -> client.deletePrivilegeSet(ps.getId()));
                    System.out.println("Deleted privilege set: " + ps.getName());
                }
            }
        }
    }

    private PrivilegeSetResponse ensurePrivilegeSet(String name, String description, List<String> privilegeIds) {
        try {
            return client.getPrivilegeSetByName(name);
        } catch (Exception ignored) {
        }

        PrivilegeSetRequest req = new PrivilegeSetRequest();
        req.setName(name);
        req.setDescription(description);
        req.setPrivilegeIds(privilegeIds);
        return client.createPrivilegeSet(req);
    }

    private LoginResponse registerUser(String username, String email, String password, String privilegeSetName) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setPassword(password);
        request.setPrivilegeSetName(privilegeSetName);

        try {
            LoginResponse response = client.register(request);
            System.out.println("Registered user: " + username + " (id=" + response.getUserId() + ")");
            return response;
        } catch (Exception e) {
            // If already exists, fetch from admin endpoints
            UserResponse existing = client.getUserByUsername(username);
            LoginResponse r = new LoginResponse();
            r.setUsername(existing.getUsername());
            r.setUserId(existing.getId());
            r.setPrivilegeSetId(existing.getPrivilegeSetId());
            r.setPrivilegeSetName(existing.getPrivilegeSetName());
            System.out.println("User already exists, reusing: " + username + " (id=" + existing.getId() + ")");
            return r;
        }
    }

    private GroupResponse ensureGroup(String name, String description, List<String> userIds) {
        try {
            return client.getGroupByName(name);
        } catch (Exception ignored) {
        }

        GroupRequest req = new GroupRequest();
        req.setName(name);
        req.setDescription(description);
        req.setUserIds(userIds);
        return client.createGroup(req);
    }

    private AccessControlListResponse ensureAcl(
        String name,
        String description,
        Map<String, String> association
    ) {
        try {
            return client.getAclByName(name);
        } catch (Exception ignored) {
        }

        AccessControlListRequest req = new AccessControlListRequest();
        req.setName(name);
        req.setDescription(description);
        req.setAssociation(association);
        return client.createAcl(req);
    }

    private DocumentClassResponse ensureDocumentClass(
        String name,
        String displayName,
        String description,
        String accessControlListId,
        boolean classLevelAcl,
        List<DocumentClassRequest.AttributeDefinition> attributes
    ) {
        try {
            return client.getDocumentClassByName(name);
        } catch (Exception ignored) {
        }

        DocumentClassRequest req = new DocumentClassRequest();
        req.setName(name);
        req.setDisplayName(displayName);
        req.setDescription(description);
        req.setAccessControlListId(accessControlListId);
        req.setClassLevelAcl(classLevelAcl);
        req.setAttributes(attributes);

        return client.createDocumentClass(req);
    }

    private static DocumentClassRequest.AttributeDefinition requiredStringAttr(String name, String displayName) {
        DocumentClassRequest.AttributeDefinition def = new DocumentClassRequest.AttributeDefinition();
        def.setName(name);
        def.setDisplayName(displayName);
        def.setType("STRING");
        def.setRequired(true);
        def.setIndexed(true);
        return def;
    }

    private DocumentResponse createSampleDocument(String documentClassId, String accessControlListId, Map<String, Object> attributes) {
        DocumentRequest req = new DocumentRequest();
        req.setDocumentClassId(documentClassId);
        req.setAccessControlListId(accessControlListId);
        req.getAttributes().putAll(attributes);

        DocumentResponse response = client.createDocument(req);
        System.out.println("Created document: " + response.getId() + " title=" + response.getAttributes().get("title") + " acl=" + response.getAccessControlListId());
        return response;
    }

    private void uploadAttachmentsBestEffort(String documentId, List<String> filePaths) {
        List<File> files = new ArrayList<>();
        for (String path : filePaths) {
            File f = new File(path);
            if (f.exists() && f.isFile()) {
                files.add(f);
            }
        }

        if (files.isEmpty()) {
            System.out.println("Skipping upload for document " + documentId + " (no valid files found)");
            return;
        }

        DocumentLockResponse lock = client.lockDocument(documentId, 900);
        try {
            for (File file : files) {
                uploadAttachmentMultipartBestEffort(documentId, file, lock.getLockId());
            }
        } finally {
            safeRun(() -> client.unlockDocument(documentId, lock.getLockId()));
        }
    }

    private void uploadAttachmentMultipartBestEffort(String documentId, File file, String lockId) {
        if (file == null || !file.exists() || !file.isFile()) {
            return;
        }

        try {
            String contentType = "application/octet-stream";

            MultipartUploadInitRequest initRequest = new MultipartUploadInitRequest();
            initRequest.setFileName(file.getName());
            initRequest.setContentType(contentType);
            initRequest.setFileSize(file.length());

            MultipartUploadInitResponse initResponse = client.initMultipartUpload(documentId, initRequest, lockId);
            String sessionId = initResponse.getSessionId();

            int partSizeBytes = initResponse.getPartSizeBytes() != null ? initResponse.getPartSizeBytes() : 10 * 1024 * 1024;
            long totalSize = file.length();
            int totalParts = (int) Math.ceil((double) totalSize / (double) partSizeBytes);
            if (totalParts <= 0) {
                System.out.println("Invalid file size for multipart upload: " + file);
                return;
            }

            HttpClient httpClient = HttpClient.newHttpClient();
            List<MultipartCompletedPart> completedParts = new ArrayList<>();

            try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                byte[] buffer = new byte[partSizeBytes];
                for (int partNumber = 1; partNumber <= totalParts; partNumber++) {
                    long offset = (long) (partNumber - 1) * partSizeBytes;
                    raf.seek(offset);
                    int read = raf.read(buffer);
                    if (read <= 0) {
                        break;
                    }

                    byte[] partBytes = read == buffer.length ? buffer : java.util.Arrays.copyOf(buffer, read);
                    MultipartPresignPartResponse presign = client.presignMultipartUploadPart(documentId, sessionId, partNumber);

                    HttpRequest putRequest = HttpRequest.newBuilder()
                            .uri(URI.create(presign.getPresignedUrl()))
                            .PUT(HttpRequest.BodyPublishers.ofByteArray(partBytes))
                            .build();

                    HttpResponse<Void> putResponse = httpClient.send(putRequest, HttpResponse.BodyHandlers.discarding());
                    if (putResponse.statusCode() < 200 || putResponse.statusCode() >= 300) {
                        throw new RuntimeException("Part upload failed. status=" + putResponse.statusCode() + ", part=" + partNumber);
                    }

                    String etag = putResponse.headers().firstValue("ETag")
                            .orElse(putResponse.headers().firstValue("Etag").orElse(null));
                    if (etag == null || etag.isBlank()) {
                        throw new RuntimeException("Missing ETag for part " + partNumber);
                    }

                    MultipartCompletedPart completedPart = new MultipartCompletedPart();
                    completedPart.setPartNumber(partNumber);
                    completedPart.setETag(etag);
                    completedParts.add(completedPart);
                }
            }

            if (completedParts.isEmpty()) {
                throw new RuntimeException("No parts uploaded.");
            }

            MultipartUploadCompleteRequest completeRequest = new MultipartUploadCompleteRequest();
            completeRequest.setParts(completedParts);

            MultipartUploadCompleteResponse completeResponse = client.completeMultipartUpload(documentId, sessionId, completeRequest, lockId);
            System.out.println("Uploaded attachment via multipart: doc=" + documentId + " file=" + file.getName() + " attachmentId=" + completeResponse.getAttachmentId());
        } catch (Exception e) {
            System.out.println("Multipart upload failed for file " + file.getName() + ": " + e.getMessage());
        }
    }

    private static <T> List<T> safeList(SupplierWithException<List<T>> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            return null;
        }
    }

    private static void safeRun(RunnableWithException runnable) {
        try {
            runnable.run();
        } catch (Exception ignored) {
        }
    }

    private record SeededUser(String username, String password) {}

    @FunctionalInterface
    private interface SupplierWithException<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    private interface RunnableWithException {
        void run() throws Exception;
    }
}
