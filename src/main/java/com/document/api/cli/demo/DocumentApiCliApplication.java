package com.document.api.cli.demo;

import com.claims.documentapi.DocumentApiClient;
import com.claims.documentapi.dto.*;

import java.util.ArrayList;
import java.util.List;
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
        System.out.println("2. Create Document Class");
        System.out.println("3. Modify Document Class");
        System.out.println("4. List Users");
        System.out.println("5. List Groups");
        System.out.println("6. List Privileges");
        System.out.println("7. Create Privilege");
        System.out.println("8. Delete Privilege");
        System.out.println("9. List Documents");
        System.out.println("10. Create Document");
        System.out.println("11. Logout");
        System.out.println("12. Exit");
        System.out.print("Choose option: ");

        int choice = getIntInput();

        switch (choice) {
            case 1:
                listDocumentClasses();
                break;
            case 2:
                createDocumentClass();
                break;
            case 3:
                modifyDocumentClass();
                break;
            case 4:
                listUsers();
                break;
            case 5:
                listGroups();
                break;
            case 6:
                listPrivileges();
                break;
            case 7:
                createPrivilege();
                break;
            case 8:
                deletePrivilege();
                break;
            case 9:
                listDocuments();
                break;
            case 10:
                createDocument();
                break;
            case 11:
                logout();
                break;
            case 12:
                System.out.println("Goodbye!");
                System.exit(0);
                break;
            default:
                System.out.println("Invalid option!");
        }
    }

    private void showUserMenu() {
        System.out.println("\n--- User Menu ---");
        System.out.println("1. Create Document");
        System.out.println("2. Delete Document");
        System.out.println("3. Search Documents");
        System.out.println("4. List My Documents");
        System.out.println("5. Add Attachment to Document");
        System.out.println("6. Logout");
        System.out.println("7. Exit");
        System.out.print("Choose option: ");

        int choice = getIntInput();

        switch (choice) {
            case 1:
                createDocument();
                break;
            case 2:
                deleteDocument();
                break;
            case 3:
                searchDocuments();
                break;
            case 4:
                listDocuments();
                break;
            case 5:
                addAttachment();
                break;
            case 6:
                logout();
                break;
            case 7:
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
                System.out.println("ID: " + docClass.getId());
                System.out.println("Name: " + docClass.getName());
                System.out.println("Display Name: " + docClass.getDisplayName());
                System.out.println("Description: " + docClass.getDescription());
                System.out.println("Attributes: " + (docClass.getAttributes() != null ? docClass.getAttributes().size() : 0));
                System.out.println("Created By: " + docClass.getCreatedBy());
                System.out.println("---");
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
            DocumentClassResponse current = client.getDocumentClass(id);
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
                requestAttr.setName(currentAttr.getName());
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
                System.out.println("ID: " + doc.getId());
                System.out.println("Name: " + doc.getName());
                System.out.println("Class: " + doc.getDocumentClassName());
                System.out.println("Status: " + doc.getStatus());
                System.out.println("Created By: " + doc.getCreatedBy());
                System.out.println("Created At: " + doc.getCreatedAt());
                System.out.println("---");
            }

        } catch (Exception e) {
            System.out.println("Failed to list documents: " + e.getMessage());
        }
    }

    private void createDocument() {
        try {
            System.out.print("Document Name: ");
            String name = scanner.nextLine();
            System.out.print("Document Class ID: ");
            String classId = scanner.nextLine();

            DocumentRequest request = new DocumentRequest();
            request.setName(name);
            request.setDocumentClassId(classId);

            DocumentResponse response = client.createDocument(request);
            System.out.println("Document created successfully!");
            System.out.println("ID: " + response.getId());

        } catch (Exception e) {
            System.out.println("Failed to create document: " + e.getMessage());
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

    private void searchDocuments() {
        try {
            System.out.print("Search term: ");
            String searchTerm = scanner.nextLine();

            // For demo purposes, we'll just list all documents
            // In a real implementation, you'd use the search API
            List<DocumentResponse> documents = client.getDocuments();
            System.out.println("\n--- Search Results (" + documents.size() + ") ---");

            for (DocumentResponse doc : documents) {
                if (doc.getName().toLowerCase().contains(searchTerm.toLowerCase())) {
                    System.out.println("ID: " + doc.getId());
                    System.out.println("Name: " + doc.getName());
                    System.out.println("Class: " + doc.getDocumentClassName());
                    System.out.println("---");
                }
            }

        } catch (Exception e) {
            System.out.println("Failed to search documents: " + e.getMessage());
        }
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
