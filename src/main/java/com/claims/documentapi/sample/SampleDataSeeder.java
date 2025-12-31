package com.claims.documentapi.sample;

import com.claims.documentapi.DocumentApiClient;
import com.claims.documentapi.dto.*;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class SampleDataSeeder {

    private static final String PREFIX = "SAMPLE_";

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
                "/home/mohamed/Documents/SampleDocs/sample2.pdf"
            );

        SampleDataSeeder seeder = new SampleDataSeeder(baseUrl);
        seeder.run(adminUsername, adminPassword, filePaths);
    }

    public void run(String adminUsername, String adminPassword, List<String> filePaths) {
        loginAsAdmin(adminUsername, adminPassword);

        cleanupSampleData();

        // Privileges are global; privilege sets reference privilege IDs
        Map<String, String> privilegeNameToId = loadPrivilegeNameToId();

        PrivilegeSetResponse readOnly = ensurePrivilegeSet(
            PREFIX + "ReadOnly",
            "Seeded read-only privilege set (search + download)",
            privilegeIds(privilegeNameToId, "LOGIN", "SEARCH_CLAIMS", "READ_CLAIM", "DOWNLOAD_DOCUMENT")
        );
        System.out.println("Created PrivilegeSet: " + readOnly.getName());


        PrivilegeSetResponse readWrite = ensurePrivilegeSet(
            PREFIX + "ReadWrite",
            "Seeded read-write privilege set (create + upload + search + download)",
            privilegeIds(privilegeNameToId, "LOGIN", "SEARCH_CLAIMS", "READ_CLAIM", "CREATE_CLAIM", "UPLOAD_DOCUMENT", "DOWNLOAD_DOCUMENT")
        );
        System.out.println("Created PrivilegeSet: " + readWrite.getName());

        // Create 4 users (via /api/auth/register)
        List<LoginResponse> users = new ArrayList<>();
        users.add(registerUser(PREFIX + "user1", PREFIX.toLowerCase() + "user1@example.com", "UserPass123!", readOnly.getName()));
        users.add(registerUser(PREFIX + "user2", PREFIX.toLowerCase() + "user2@example.com", "UserPass123!", readOnly.getName()));
        users.add(registerUser(PREFIX + "user3", PREFIX.toLowerCase() + "user3@example.com", "UserPass123!", readWrite.getName()));
        users.add(registerUser(PREFIX + "user4", PREFIX.toLowerCase() + "user4@example.com", "UserPass123!", readWrite.getName()));

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
            Map.of(groupA.getId(), readWrite.getId())
        );

        AccessControlListResponse aclB = ensureAcl(
            PREFIX + "ACL_B",
            "Seeded ACL B",
            Map.of(groupB.getId(), readOnly.getId())
        );

        // Create 2 sample document classes
        DocumentClassResponse classA = ensureDocumentClass(
            PREFIX + "ClassA",
            "Sample Class A",
            "Seeded class A",
            null,
            false,
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

        // Create documents and assign ACLs per document
        DocumentResponse doc1 = createSampleDocument(classA.getId(), aclA.getId(), Map.of("title", PREFIX + "Doc1", "category", "A"));
        DocumentResponse doc2 = createSampleDocument(classA.getId(), aclB.getId(), Map.of("title", PREFIX + "Doc2", "category", "B"));
        DocumentResponse doc3 = createSampleDocument(classB.getId(), aclA.getId(), Map.of("title", PREFIX + "Doc3", "department", "HR"));

        // Upload attachments (best-effort; skip missing files)
        uploadAttachmentsBestEffort(doc1.getId(), filePaths);
        uploadAttachmentsBestEffort(doc2.getId(), filePaths);

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

    private void cleanupSampleData() {
        System.out.println("\n=== Cleanup (prefix: " + PREFIX + ") ===");

        // Documents (delete by seeded attribute)
        List<DocumentResponse> documents = safeList(client::getDocuments);
        if (documents != null) {
            for (DocumentResponse d : documents) {
                Object title = d.getAttributes() != null ? d.getAttributes().get("title") : null;
                if (title instanceof String && ((String) title).startsWith(PREFIX)) {
                    safeRun(() -> client.deleteDocument(d.getId()));
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

    private Map<String, String> loadPrivilegeNameToId() {
        List<PrivilegeResponse> privileges = client.getPrivileges();
        if (privileges == null) {
            return Map.of();
        }
        return privileges.stream()
            .filter(p -> p.getName() != null && p.getId() != null)
            .collect(Collectors.toMap(PrivilegeResponse::getName, PrivilegeResponse::getId, (a, b) -> a));
    }

    private List<String> privilegeIds(Map<String, String> nameToId, String... names) {
        List<String> ids = new ArrayList<>();
        for (String n : names) {
            String id = nameToId.get(n);
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
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

        List<DocumentAttachmentDto> uploaded = client.uploadAttachments(documentId, files);
        System.out.println("Uploaded " + uploaded.size() + " attachments to document " + documentId);
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

    @FunctionalInterface
    private interface SupplierWithException<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    private interface RunnableWithException {
        void run() throws Exception;
    }
}
