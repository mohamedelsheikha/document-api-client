package com.document.api.cli.demo;

import com.claims.documentapi.DocumentApiClient;
import com.claims.documentapi.dto.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Command-line interface for Document Management API
 * Demonstrates all features of the document-api-client library
 */
public class DocumentApiCliApplication {

    private static final Scanner scanner = new Scanner(System.in);
    private final DocumentApiClient client;
    private LoginResponse currentUser;

    public static void main(String[] args) {
        String apiUrl = args.length > 0 ? args[0] : "http://localhost:8080/api";

        DocumentApiCliApplication app = new DocumentApiCliApplication(apiUrl);
        app.start();
    }

    public DocumentApiCliApplication(String apiUrl) {
        client = new DocumentApiClient(apiUrl);
    }

    public void start() {
        System.out.println("=== Document Management API CLI ===");
        System.out.println("API URL: http://localhost:8080/api");
        System.out.println();

        // Interactive mode
        interactiveMode();
    }

    private void interactiveMode() {
        while (true) {
            if (currentUser == null) {
                showGuestMenu();
            } else if ("Administrator".equalsIgnoreCase(currentUser.getRole())) {
                showAdminMenu();
            } else {
                showUserMenu();
            }
        }
    }

    private void showGuestMenu() {
        System.out.println("\n--- Guest Menu ---");
        System.out.println("1. Login");
        System.out.println("2. Exit");
        System.out.print("Choose option: ");

        int choice = getIntInput();

        switch (choice) {
            case 1:
                login();
                break;
            case 2:
                System.out.println("Goodbye!");
                System.exit(0);
                break;
            default:
                System.out.println("Invalid option!");
        }
    }

    private void showAdminMenu() {
        System.out.println("\n--- Admin Menu ---");
        System.out.println("1. List Document Classes");
        System.out.println("2. Get Single Document Class");
        System.out.println("3. Create Document Class");
        System.out.println("4. Modify Document Class");
        System.out.println("5. List Users");
        System.out.println("6. List Groups");
        System.out.println("7. List Privileges");
        System.out.println("8. Create Privilege");
        System.out.println("9. Delete Privilege");
        System.out.println("10. List Documents");
        System.out.println("11. Create Document");
        System.out.println("12. Upload attachment to an existing document");
        System.out.println("13. Logout");
        System.out.println("14. Exit");
        System.out.print("Choose option: ");

        int choice = getIntInput();

        switch (choice) {
            case 1:
                listDocumentClasses();
                break;
            case 2:
                getDocumentClassInfo();
                break;
            case 3:
                createDocumentClass();
                break;
            case 4:
                modifyDocumentClass();
                break;
            case 5:
                listUsers();
                break;
            case 6:
                listGroups();
                break;
            case 7:
                listPrivileges();
                break;
            case 8:
                createPrivilege();
                break;
            case 9:
                deletePrivilege();
                break;
            case 10:
                listDocuments();
                break;
            case 11:
                createDocument();
                break;
            case 12:
                uploadAttachmentToExistingDocument();
                break;
            case 13:
                logout();
                break;
            case 14:
                System.out.println("Goodbye!");
                System.exit(0);
                break;
            default:
                System.out.println("Invalid option!");
        }
    }

    private void getDocumentClassInfo() {
        System.out.println("\n--- Document Class Info menu ---");
        System.out.println("1. By ID");
        System.out.println("2. By name");
        System.out.println("3. Exit");
        System.out.print("Choose option: ");

        int choice = getIntInput();

        switch (choice) {
            case 1:
                getDocumentClassInfoById();
                break;
            case 2:
                getDocumentClassInfoByName();
                break;
            case 3:
                System.out.println("Goodbye!");
                System.exit(0);
                break;
            default:
                System.out.println("Invalid option!");
        }

    }

    private void getDocumentClassInfoByName() {
        System.out.print("Document Class Name: ");
        String name = scanner.nextLine();
        try {
            DocumentClassResponse documentClassInfo = client.getDocumentClassByName(name);
            printDocumentClassInfo(documentClassInfo);
        } catch (Exception e) {
            System.out.println("Get Document Class by name failed: " + e.getMessage());
        }

    }

    private void getDocumentClassInfoById() {
        System.out.print("Document Class ID: ");
        String id = scanner.nextLine();
        try {
            DocumentClassResponse documentClassInfo = client.getDocumentClassById(id);
            printDocumentClassInfo(documentClassInfo);
        } catch (Exception e) {
            System.out.println("Get Document Class by ID failed: " + e.getMessage());
        }
    }

    private void printDocumentClassInfo(DocumentClassResponse docClass) {
        System.out.println("ID: " + docClass.getId());
        System.out.println("Name: " + docClass.getName());
        System.out.println("Display Name: " + docClass.getDisplayName());
        System.out.println("Description: " + docClass.getDescription());
        System.out.println("Created By: " + docClass.getCreatedBy());
        System.out.println("Attributes: " + (docClass.getAttributes() != null ? docClass.getAttributes().size() : 0));
        printAttributes(docClass.getAttributes());
        System.out.println("---");
    }

    private void printAttributes(List<DocumentClassResponse.AttributeDefinition> attributes) {
        System.out.println("\n--- Attributes ---");
        for (DocumentClassResponse.AttributeDefinition attr : attributes) {
            System.out.println("ID: " + attr.getId());
            System.out.println("Display name: " + attr.getDisplayName());
            System.out.println("Type: " + attr.getType());
            System.out.println("Length: " + attr.getLength());
            System.out.println("Required: " + attr.isRequired());
            System.out.println("Multi valued: " + attr.isMultiValue());
            System.out.println("---");
        }
    }

    private void showUserMenu() {
        System.out.println("\n--- User Menu ---");
        System.out.println("1. Create Document");
        System.out.println("2. Upload attachment to existing document");
        System.out.println("3. Delete Document");
        System.out.println("4. Search Documents");
        System.out.println("5. List My Documents");
        System.out.println("6. Add Attachment to Document");
        System.out.println("7. Logout");
        System.out.println("8. Exit");
        System.out.print("Choose option: ");

        int choice = getIntInput();

        switch (choice) {
            case 1:
                createDocument();
                break;
            case 2:
                uploadAttachmentToExistingDocument();
                break;
            case 3:
                deleteDocument();
                break;
            case 4:
                searchDocuments();
                break;
            case 5:
                listDocuments();
                break;
            case 6:
                addAttachment();
                break;
            case 7:
                logout();
                break;
            case 8:
                System.out.println("Goodbye!");
                System.exit(0);
                break;
            default:
                System.out.println("Invalid option!");
        }
    }

    private void login() {
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        try {
            LoginRequest request = new LoginRequest();
            request.setUsername(username);
            request.setPassword(password);

            LoginResponse response = client.login(request);
            currentUser = response;
            client.setAuthToken(response.getToken());

            System.out.println("Login successful!");
            System.out.println("Welcome, " + response.getUsername() + "!");
            System.out.println("Role: " + response.getRole());
            System.out.println("User: " + response);

        } catch (Exception e) {
            System.out.println("Login failed: " + e.getMessage());
        }
    }

    private void logout() {
        client.clearAuth();
        currentUser = null;
        System.out.println("Logged out successfully!");
    }

    private void listDocumentClasses() {
        try {
            List<DocumentClassResponse> classes = client.getDocumentClasses();
            System.out.println("\n--- Document Classes (" + classes.size() + ") ---");

            for (DocumentClassResponse docClass : classes) {
                printDocumentClassInfo(docClass);
            }

        } catch (Exception e) {
            System.out.println("Failed to list document classes: " + e.getMessage());
        }
    }

    private void createDocumentClass() {
        try {
            System.out.print("Name: ");
            String name = scanner.nextLine();
            System.out.print("Display Name: ");
            String displayName = scanner.nextLine();
            System.out.print("Description: ");
            String description = scanner.nextLine();

            // Handle attribute definitions
            List<DocumentClassRequest.AttributeDefinition> attributeDefinitions = new ArrayList<>();
            System.out.println("Add attribute definitions (enter empty name to finish):");

            while (true) {
                System.out.print("Attribute Name (or empty to finish): ");
                String attrName = scanner.nextLine().trim();
                if (attrName.isEmpty()) {
                    break;
                }

                DocumentClassRequest.AttributeDefinition attrDef = new DocumentClassRequest.AttributeDefinition();
                attrDef.setName(attrName);

                System.out.print("Attribute Display Name: ");
                attrDef.setDisplayName(scanner.nextLine().trim());

                System.out.print("Attribute Type (STRING, NUMBER, DATE, BOOLEAN): ");
                attrDef.setType(scanner.nextLine().trim());

                System.out.print("Required (true/false): ");
                attrDef.setRequired(Boolean.parseBoolean(scanner.nextLine().trim()));

                System.out.print("Indexed (true/false): ");
                attrDef.setIndexed(Boolean.parseBoolean(scanner.nextLine().trim()));

                System.out.print("Validation Rules (optional): ");
                attrDef.setValidationPattern(scanner.nextLine().trim());

                attributeDefinitions.add(attrDef);
                System.out.println("Attribute added. " + attributeDefinitions.size() + " total attributes.");
            }

            DocumentClassRequest request = new DocumentClassRequest();
            request.setName(name);
            request.setDisplayName(displayName);
            request.setDescription(description);
            request.setAttributes(attributeDefinitions);

            DocumentClassResponse response = client.createDocumentClass(request);
            System.out.println("Document class created successfully!");
            System.out.println("ID: " + response.getId());

        } catch (Exception e) {
            System.out.println("Failed to create document class: " + e.getMessage());
        }
    }

    private void modifyDocumentClass() {
        try {
            System.out.print("Document Class ID: ");
            String id = scanner.nextLine();

            // First, get the current document class
            DocumentClassResponse current = client.getDocumentClassById(id);
            System.out.println("Current Document Class:");
            System.out.println("Name: " + current.getName());
            System.out.println("Display Name: " + current.getDisplayName());
            System.out.println("Description: " + current.getDescription());

            System.out.print("New Display Name (leave empty to keep current): ");
            String displayName = scanner.nextLine();
            System.out.print("New Description (leave empty to keep current): ");
            String description = scanner.nextLine();

            DocumentClassRequest request = new DocumentClassRequest();
            request.setName(current.getName());
            request.setDisplayName(displayName.isEmpty() ? current.getDisplayName() : displayName);
            request.setDescription(description.isEmpty() ? current.getDescription() : description);

            // copy response attributes into request attributes
            List<DocumentClassRequest.AttributeDefinition> attributeDefinitions = new ArrayList<>();
            for (DocumentClassResponse.AttributeDefinition currentAttr : current.getAttributes()) {
                DocumentClassRequest.AttributeDefinition requestAttr = new DocumentClassRequest.AttributeDefinition();
                requestAttr.setName(currentAttr.getId());
                requestAttr.setType(currentAttr.getType());
                requestAttr.setIndexed(currentAttr.isIndexed());
                requestAttr.setRequired((currentAttr.isRequired()));
                requestAttr.setLength(currentAttr.getLength());
                requestAttr.setMultiValue(currentAttr.isMultiValue());
                attributeDefinitions.add(requestAttr);
            }
            request.setAttributes(attributeDefinitions);

            DocumentClassResponse response = client.updateDocumentClass(id, request);
            System.out.println("Document class updated successfully!");

        } catch (Exception e) {
            System.out.println("Failed to modify document class: " + e.getMessage());
        }
    }

    private void listUsers() {
        try {
            List<UserResponse> users = client.getUsers();
            System.out.println("\n--- Users (" + users.size() + ") ---");

            for (UserResponse user : users) {
                System.out.println("ID: " + user.getId());
                System.out.println("Username: " + user.getUsername());
                System.out.println("Email: " + user.getEmail());
                System.out.println("Role: " + user.getRole());
                System.out.println("Enabled: " + user.isEnabled());
                System.out.println("Privilege Set ID: " + user.getPrivilegeSetId());
                System.out.println("Privilege Set Name: " + user.getPrivilegeSetName());
                System.out.println("Created at: " + user.getCreatedAt());
                System.out.println("---");
            }

        } catch (Exception e) {
            System.out.println("Failed to list users: " + e.getMessage());
        }
    }

    private void listGroups() {
        try {
            List<GroupResponse> groups = client.getGroups();
            System.out.println("\n--- Groups (" + groups.size() + ") ---");

            for (GroupResponse group : groups) {
                System.out.println("ID: " + group.getId());
                System.out.println("Name: " + group.getName());
                System.out.println("Description: " + group.getDescription());
                System.out.println("Created By: " + group.getCreatedBy());
                System.out.println("---");
            }

        } catch (Exception e) {
            System.out.println("Failed to list groups: " + e.getMessage());
        }
    }

    private void listPrivileges() {
        try {
            List<PrivilegeResponse> privileges = client.getPrivileges();
            System.out.println("\n--- Privileges (" + privileges.size() + ") ---");

            for (PrivilegeResponse privilege : privileges) {
                System.out.println("ID: " + privilege.getId());
                System.out.println("Name: " + privilege.getName());
                System.out.println("Description: " + privilege.getDescription());
                System.out.println("---");
            }

        } catch (Exception e) {
            System.out.println("Failed to list privileges: " + e.getMessage());
        }
    }

    private void createPrivilege() {
        try {
            System.out.print("Name: ");
            String name = scanner.nextLine();
            System.out.print("Description: ");
            String description = scanner.nextLine();

            PrivilegeRequest request = new PrivilegeRequest();
            request.setName(name);
            request.setDescription(description);

            PrivilegeResponse response = client.createPrivilege(request);
            System.out.println("Privilege created successfully!");
            System.out.println("ID: " + response.getId());

        } catch (Exception e) {
            System.out.println("Failed to create privilege: " + e.getMessage());
        }
    }

    private void deletePrivilege() {
        try {
            System.out.print("Privilege ID: ");
            String id = scanner.nextLine();

            client.deletePrivilege(id);
            System.out.println("Privilege deleted successfully!");

        } catch (Exception e) {
            System.out.println("Failed to delete privilege: " + e.getMessage());
        }
    }

    private void listDocuments() {
        try {
            List<DocumentResponse> documents = client.getDocuments();
            System.out.println("\n--- Documents (" + documents.size() + ") ---");

            for (DocumentResponse doc : documents) {
                printDocument(doc);
                
                // Retrieve and display attachments with presigned URLs
                try {
                    List<DocumentDto> attachments = client.getDocumentAttachments(doc.getId());
                    if (attachments != null && !attachments.isEmpty()) {
                        System.out.println("--- Attachments (" + attachments.size() + ") ---");
                        for (DocumentDto attachment : attachments) {
                            System.out.println("  File: " + attachment.getFileName());
                            System.out.println("  Size: " + attachment.getFileSize() + " bytes");
                            System.out.println("  Type: " + attachment.getContentType());
                            
                            // Get presigned URL for this attachment
                            try {
                                // Use the database attachment ID directly
                                String attachmentId = attachment.getId();
                                
                                if (attachmentId != null) {
                                    String downloadUrl = client.getAttachmentDownloadUrl(doc.getId(), attachmentId);

                                    if (downloadUrl != null && !downloadUrl.isEmpty()) {
                                        System.out.println("  Download URL: " + downloadUrl);
                                        System.out.println("  NOTE: If URL gives NoSuchKey error, the S3 key may not exist in the bucket");
                                    } else {
                                        System.out.println("  Download URL: Not available (empty response)");
                                    }
                                } else {
                                    System.out.println("  Download URL: Could not extract attachment ID");
                                }
                            } catch (Exception urlError) {
                                System.out.println("  Download URL: Failed to generate (" + urlError.getMessage() + ")");
                                urlError.printStackTrace();
                            }
                            System.out.println();
                        }
                    } else {
                        System.out.println("--- Attachments: None ---");
                    }
                } catch (Exception attachError) {
                    System.out.println("--- Attachments: Failed to retrieve (" + attachError.getMessage() + ") ---");
                }
                
                System.out.println("---");
            }

        } catch (Exception e) {
            System.out.println("Failed to list documents: " + e.getMessage());
        }
    }

    private void printDocument(DocumentResponse doc) {
        System.out.println("ID: " + doc.getId());
        System.out.println("Class: " + doc.getDocumentClassName());
        System.out.println("Created By: " + doc.getCreatedBy());
        System.out.println("Created At: " + doc.getCreatedAt());
        System.out.println("--- Attributes ---");
        for (Map.Entry<String, Object> attr : doc.getAttributes().entrySet()) {
            System.out.println("    " + attr.getKey() + ": " + attr.getValue());
        }
    }

    private void createDocument() {
        try {
            System.out.println("Select document type:");
            System.out.println("1. Generic Document");
            System.out.println("2. Claim Document with Attachments");
            System.out.print("Choice (1-2): ");
            
            String choice = scanner.nextLine().trim();
            
            if ("2".equals(choice)) {
                createClaimDocumentWithAttachments();
            } else {
                createGenericDocument();
            }
            
        } catch (Exception e) {
            System.out.println("Failed to create document: " + e.getMessage());
        }
    }
    
    private void createGenericDocument() {
        try {
            System.out.print("Document Name: ");
            String name = scanner.nextLine();
            System.out.print("Document Class ID: ");
            String classId = scanner.nextLine();

            // find document class by id
            DocumentClassResponse docClass = client.getDocumentClassById(classId);

            DocumentRequest request = new DocumentRequest();
            request.setName(name);
            request.setDocumentClassId(classId);

            System.out.print("\n--- Document attribute entry ---  ");
            String attrVal;
            for (DocumentClassResponse.AttributeDefinition attrDef : docClass.getAttributes()) {
                System.out.print(attrDef.getDisplayName() + ": ");
                attrVal = scanner.nextLine();
                request.getAttributes().put(attrDef.getId(), attrVal);
            }

            DocumentResponse response = client.createDocument(request);
            System.out.println("Document created successfully!");
            System.out.println("ID: " + response.getId());
            printDocument(response);

        } catch (Exception e) {
            System.out.println("Failed to create generic document: " + e.getMessage());
        }
    }
    
    private void createClaimDocumentWithAttachments() {
        try {
            System.out.print("Claim Number: ");
            String claimNumber = scanner.nextLine();
            
            System.out.print("Claimant Names (comma-separated): ");
            String claimantNamesStr = scanner.nextLine();
            List<String> claimantNames = List.of(claimantNamesStr.split("\\s*,\\s*"));
            
            System.out.print("Date of Loss (YYYY-MM-DD): ");
            String dateOfLossStr = scanner.nextLine();
            java.time.LocalDate dateOfLoss = java.time.LocalDate.parse(dateOfLossStr);
            
            System.out.print("Description: ");
            String description = scanner.nextLine();

            // Handle file attachments
            List<java.io.File> attachmentFiles = new ArrayList<>();
            System.out.println("\n--- Add file attachments (optional) ---");
            
            while (true) {
                System.out.print("Enter file path (or empty to finish): ");
                String filePath = scanner.nextLine().trim();
                
                if (filePath.isEmpty()) {
                    break;
                }
                
                java.io.File file = new java.io.File(filePath);
                if (file.exists() && file.isFile()) {
                    attachmentFiles.add(file);
                    System.out.println("Added: " + file.getName() + " (" + file.length() + " bytes)");
                } else {
                    System.out.println("File not found: " + filePath);
                }
            }
            
            // Create claim document request
            ClaimDocumentRequest request = new ClaimDocumentRequest();
            request.setClaimNumber(claimNumber);
            request.setClaimantNames(claimantNames);
            request.setDateOfLoss(dateOfLoss);
            request.setDescription(description);
            
            // Upload with attachments
            ClaimDocumentResponse response;
            if (attachmentFiles.isEmpty()) {
                // Upload without files (URL-based or metadata only)
                response = client.uploadDocumentFromUrl(request, "");
            } else {
                // Upload with files
                java.io.File mainFile = attachmentFiles.get(0);
                java.io.File[] additionalFiles = attachmentFiles.subList(1, attachmentFiles.size()).toArray(new java.io.File[0]);
                response = client.uploadDocument(request, mainFile, additionalFiles);
            }
            
            System.out.println("Claim document created successfully!");
            System.out.println("ID: " + response.getId());
            System.out.println("Claim Number: " + response.getClaimNumber());
            System.out.println("Description: " + response.getDescription());
            
            if (response.getDocuments() != null && !response.getDocuments().isEmpty()) {
                System.out.println("Uploaded Files:");
                for (DocumentDto doc : response.getDocuments()) {
                    System.out.println("  - " + doc.getFileName() + " (" + doc.getFileSize() + " bytes)");
                }
            }

        } catch (Exception e) {
            System.out.println("Failed to create claim document: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void deleteDocument() {
        try {
            System.out.print("Document ID: ");
            String id = scanner.nextLine();

            client.deleteDocument(id);
            System.out.println("Document deleted successfully!");

        } catch (Exception e) {
            System.out.println("Failed to delete document: " + e.getMessage());
        }
    }
    
    private void uploadAttachmentToExistingDocument() {
        try {
            // First, list existing documents for user to choose from
            System.out.println("\n--- Existing Documents ---");
            List<DocumentResponse> documents = client.getDocuments();
            
            if (documents.isEmpty()) {
                System.out.println("No documents found. Please create a document first.");
                return;
            }
            
            // Display documents for selection
            for (int i = 0; i < documents.size(); i++) {
                DocumentResponse doc = documents.get(i);
                System.out.printf("%d. ID: %s | Class: %s | Created: %s%n", 
                    i + 1, doc.getId(), doc.getDocumentClassName(), doc.getCreatedAt());
            }
            
            System.out.print("\nSelect document by number (1-" + documents.size() + "): ");
            int choice = getIntInput();
            
            if (choice < 1 || choice > documents.size()) {
                System.out.println("Invalid selection!");
                return;
            }
            
            DocumentResponse selectedDocument = documents.get(choice - 1);
            String documentId = selectedDocument.getId();
            
            System.out.println("\nSelected document: " + selectedDocument.getId());
            System.out.println("Document Class: " + selectedDocument.getDocumentClassName());
            
            // Handle file attachments
            List<java.io.File> attachmentFiles = new ArrayList<>();
            System.out.println("\n--- Add file attachments ---");
            
            while (true) {
                System.out.print("Enter file path (or empty to finish): ");
                String filePath = scanner.nextLine().trim();
                
                if (filePath.isEmpty()) {
                    break;
                }
                
                java.io.File file = new java.io.File(filePath);
                if (file.exists() && file.isFile()) {
                    attachmentFiles.add(file);
                    System.out.println("Added: " + file.getName() + " (" + file.length() + " bytes)");
                } else {
                    System.out.println("File not found: " + filePath);
                }
            }
            
            if (attachmentFiles.isEmpty()) {
                System.out.println("No files selected. Upload cancelled.");
                return;
            }
            
            // Upload attachments
            System.out.println("\nUploading " + attachmentFiles.size() + " file(s) to document " + documentId + "...");
            
            if (attachmentFiles.size() == 1) {
                // Single file upload
                java.io.File file = attachmentFiles.get(0);
                UploadResponse response = client.uploadAttachmentToDocumentId(documentId, file);
                System.out.println("Attachment uploaded successfully!");
                System.out.println("Response: " + response);
            } else {
                // Multiple files upload
                UploadResponse response = client.uploadMultipleAttachments(documentId, attachmentFiles);
                System.out.println("Multiple attachments uploaded successfully!");
                System.out.println("Response: " + response);
            }
            
            // Show updated document info
            System.out.println("\n--- Updated Document ---");
            DocumentResponse updatedDoc = client.getDocument(documentId);
            printDocument(updatedDoc);
            
        } catch (Exception e) {
            System.out.println("Failed to upload attachment: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void searchDocuments() {
        System.out.println("Not implemented...");
    }

    private void addAttachment() {
        try {
            System.out.print("Document ID: ");
            String docId = scanner.nextLine();
            System.out.print("Attachment file path: ");
            String filePath = scanner.nextLine();

            // This is a placeholder for attachment functionality
            // In a real implementation, you'd upload the file
            System.out.println("Attachment feature would upload: " + filePath);
            System.out.println("To document: " + docId);
            System.out.println("(Note: Actual file upload implementation required)");

        } catch (Exception e) {
            System.out.println("Failed to add attachment: " + e.getMessage());
        }
    }

    private int getIntInput() {
        try {
            return Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
