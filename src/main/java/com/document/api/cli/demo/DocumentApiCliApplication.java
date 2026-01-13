package com.document.api.cli.demo;

import com.claims.documentapi.DocumentApiClient;
import com.claims.documentapi.dto.*;
import lombok.NonNull;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
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

    private String lastLockedDocumentId;
    private String lastLockId;

    public static void main(String[] args) {
        String apiUrl = args.length > 0 ? args[0] : "http://localhost:5000/api";

        DocumentApiCliApplication app = new DocumentApiCliApplication(apiUrl);
        app.start();
    }

    public DocumentApiCliApplication(String apiUrl) {
        client = new DocumentApiClient(apiUrl);
    }

    public void start() {
        System.out.println("=== Document Management API CLI ===");
        System.out.println("API URL: " + client.getBaseApiUrl());
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
        System.out.println("2. List Tenants");
        System.out.println("3. Exit");
        System.out.print("Choose option: ");

        int choice = getIntInput();

        switch (choice) {
            case 1:
                login();
                break;
            case 2:
                listTenantsTopLevel();
                break;
            case 3:
                System.out.println("Goodbye!");
                System.exit(0);
                break;
            default:
                System.out.println("Invalid option!");
        }
    }

    private void showAdminMenu() {
        System.out.println("\n--- Admin Menu ---");
        System.out.println("1. Authentication Management");
        System.out.println("2. Authorization Management");
        System.out.println("3. Data Model Management");
        System.out.println("4. Document Management");
        System.out.println("5. List Tenants");
        System.out.println("6. Logout");
        System.out.println("7. Exit");
        System.out.print("Choose option: ");

        int choice = getIntInput();

        switch (choice) {
            case 1:
                showAuthenticationManagementMenu();
                break;
            case 2:
                showAuthorizationManagementMenu();
                break;
            case 3:
                showDataModelManagementMenu();
                break;
            case 4:
                showDocumentManagementMenu();
                break;
            case 5:
                listTenantsTopLevel();
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

    private void listPrivilegeSets() {
        try {
            List<PrivilegeSetResponse> sets = client.getPrivilegeSets();
            System.out.println("\n--- Privilege Sets (" + (sets != null ? sets.size() : 0) + ") ---");
            if (sets == null || sets.isEmpty()) {
                return;
            }
            for (PrivilegeSetResponse ps : sets) {
                System.out.println("ID: " + ps.getId());
                System.out.println("Name: " + ps.getName());
                System.out.println("Description: " + ps.getDescription());
                System.out.println("Privileges: " + ps.getPrivilegeIds());
                System.out.println("---");
            }
        } catch (Exception e) {
            System.out.println("Failed to list privilege sets: " + e.getMessage());
        }
    }

    private void createPrivilegeSet() {
        try {
            System.out.print("Name: ");
            String name = scanner.nextLine();
            System.out.print("Description: ");
            String description = scanner.nextLine();

            List<PrivilegeResponse> privileges = client.getPrivileges();
            if (privileges == null || privileges.isEmpty()) {
                System.out.println("No privileges found. Create privileges first.");
                return;
            }

            System.out.println("\n--- Available Privileges ---");
            for (PrivilegeResponse p : privileges) {
                System.out.println("- " + p.getId() + " | " + p.getName());
            }
            System.out.print("Enter privilege NAMES (comma-separated): ");
            String ids = scanner.nextLine().trim();
            List<String> privilegeIds = new ArrayList<>();
            if (!ids.isEmpty()) {
                for (String part : ids.split(",")) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        privilegeIds.add(trimmed);
                    }
                }
            }
            if (privilegeIds.isEmpty()) {
                System.out.println("Privilege set must include at least one privilege.");
                return;
            }

            PrivilegeSetRequest request = new PrivilegeSetRequest();
            request.setName(name);
            request.setDescription(description);
            request.setPrivilegeIds(privilegeIds);

            PrivilegeSetResponse created = client.createPrivilegeSet(request);
            System.out.println("Privilege set created successfully!");
            System.out.println("ID: " + created.getId());
            System.out.println("Name: " + created.getName());
        } catch (Exception e) {
            System.out.println("Failed to create privilege set: " + e.getMessage());
        }
    }

    private void updatePrivilegeSet() {
        try {
            System.out.print("Privilege Set ID: ");
            String id = scanner.nextLine().trim();
            System.out.print("New Name (leave empty to keep current): ");
            String name = scanner.nextLine();
            System.out.print("New Description (leave empty to keep current): ");
            String description = scanner.nextLine();

            List<PrivilegeSetResponse> sets = client.getPrivilegeSets();
            PrivilegeSetResponse current = null;
            if (sets != null) {
                for (PrivilegeSetResponse ps : sets) {
                    if (ps.getId() != null && ps.getId().equals(id)) {
                        current = ps;
                        break;
                    }
                }
            }
            if (current == null) {
                System.out.println("Privilege set not found in list. Ensure the ID is correct.");
                return;
            }

            System.out.println("Current privileges: " + current.getPrivilegeIds());
            System.out.print("New privilege NAMES (comma-separated, leave empty to keep current): ");
            String ids = scanner.nextLine().trim();
            List<String> privilegeIds = current.getPrivilegeIds() != null ? new ArrayList<>(current.getPrivilegeIds()) : new ArrayList<>();
            if (!ids.isEmpty()) {
                privilegeIds = new ArrayList<>();
                for (String part : ids.split(",")) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        privilegeIds.add(trimmed);
                    }
                }
            }
            if (privilegeIds.isEmpty()) {
                System.out.println("Privilege set must include at least one privilege.");
                return;
            }

            PrivilegeSetRequest request = new PrivilegeSetRequest();
            request.setName(name.isEmpty() ? current.getName() : name);
            request.setDescription(description.isEmpty() ? current.getDescription() : description);
            request.setPrivilegeIds(privilegeIds);

            PrivilegeSetResponse updated = client.updatePrivilegeSet(id, request);
            System.out.println("Privilege set updated successfully!");
            System.out.println("ID: " + updated.getId());
            System.out.println("Name: " + updated.getName());
        } catch (Exception e) {
            System.out.println("Failed to update privilege set: " + e.getMessage());
        }
    }

    private void deletePrivilegeSet() {
        try {
            System.out.print("Privilege Set ID: ");
            String id = scanner.nextLine().trim();
            client.deletePrivilegeSet(id);
            System.out.println("Privilege set deleted successfully!");
        } catch (Exception e) {
            System.out.println("Failed to delete privilege set: " + e.getMessage());
        }
    }

    private void listAcls() {
        try {
            List<AccessControlListResponse> acls = client.getAcls();
            System.out.println("\n--- ACLs (" + (acls != null ? acls.size() : 0) + ") ---");
            if (acls == null || acls.isEmpty()) {
                return;
            }
            for (AccessControlListResponse acl : acls) {
                System.out.println("ID: " + acl.getId());
                System.out.println("Name: " + acl.getName());
                System.out.println("Description: " + acl.getDescription());
                System.out.println("Association: " + acl.getAssociation());
                System.out.println("---");
            }
        } catch (Exception e) {
            System.out.println("Failed to list ACLs: " + e.getMessage());
        }
    }

    private void createAcl() {
        try {
            System.out.print("Name: ");
            String name = scanner.nextLine();
            System.out.print("Description: ");
            String description = scanner.nextLine();

            List<GroupResponse> groups = client.getGroups();
            if (groups == null || groups.isEmpty()) {
                System.out.println("No groups found. Create a group first.");
                return;
            }

            List<PrivilegeSetResponse> privilegeSets = client.getPrivilegeSets();
            if (privilegeSets == null || privilegeSets.isEmpty()) {
                System.out.println("No privilege sets found. Create a privilege set first.");
                return;
            }

            System.out.println("\nACL association maps PRINCIPAL -> PRIVILEGE_SET_ID.");
            System.out.println("Principals are matched by username and group name.");

            Map<String, String> association = new HashMap<>();
            while (true) {
                System.out.println("\n--- Groups ---");
                for (int i = 0; i < groups.size(); i++) {
                    GroupResponse g = groups.get(i);
                    System.out.println((i + 1) + ". " + g.getName() + " (ID: " + g.getId() + ")");
                }
                if (!association.isEmpty()) {
                    System.out.println("0. Finish and create ACL");
                }
                System.out.print("Choose a group (enter number): ");
                int groupChoice = getIntInput();
                if (!association.isEmpty() && groupChoice == 0) {
                    break;
                }
                if (groupChoice < 1 || groupChoice > groups.size()) {
                    System.out.println("Invalid choice!");
                    continue;
                }

                GroupResponse selectedGroup = groups.get(groupChoice - 1);
                String principal = selectedGroup.getName() != null ? selectedGroup.getName().trim() : "";
                if (principal.isEmpty()) {
                    System.out.println("Selected group has no name; cannot use it as a principal.");
                    continue;
                }

                System.out.println("\n--- Privilege Sets ---");
                for (int i = 0; i < privilegeSets.size(); i++) {
                    PrivilegeSetResponse ps = privilegeSets.get(i);
                    System.out.println((i + 1) + ". " + ps.getName() + " (ID: " + ps.getId() + ")");
                }

                System.out.print("Choose a privilege set (enter number): ");
                int psChoice = getIntInput();
                if (psChoice < 1 || psChoice > privilegeSets.size()) {
                    System.out.println("Invalid choice!");
                    continue;
                }

                PrivilegeSetResponse selectedPs = privilegeSets.get(psChoice - 1);
                String privilegeSetId = selectedPs.getId() != null ? selectedPs.getId().trim() : "";
                if (privilegeSetId.isEmpty()) {
                    System.out.println("Selected privilege set has no ID.");
                    continue;
                }

                association.put(principal, privilegeSetId);
                System.out.println("Added association: " + principal + " -> " + privilegeSetId);
            }

            if (association.isEmpty()) {
                System.out.println("ACL must include at least one association.");
                return;
            }

            AccessControlListRequest request = new AccessControlListRequest();
            request.setName(name);
            request.setDescription(description);
            request.setAssociation(association);

            AccessControlListResponse created = client.createAcl(request);
            System.out.println("ACL created successfully!");
            System.out.println("ID: " + created.getId());
            System.out.println("Name: " + created.getName());
            System.out.println("Association: " + created.getAssociation());
        } catch (Exception e) {
            System.out.println("Failed to create ACL: " + e.getMessage());
        }
    }

    private void updateAcl() {
        try {
            System.out.print("ACL ID: ");
            String id = scanner.nextLine().trim();

            List<AccessControlListResponse> acls = client.getAcls();
            AccessControlListResponse current = null;
            if (acls != null) {
                for (AccessControlListResponse acl : acls) {
                    if (acl.getId() != null && acl.getId().equals(id)) {
                        current = acl;
                        break;
                    }
                }
            }
            if (current == null) {
                System.out.println("ACL not found in list. Ensure the ID is correct.");
                return;
            }

            System.out.print("New Name (leave empty to keep current): ");
            String name = scanner.nextLine();
            System.out.print("New Description (leave empty to keep current): ");
            String description = scanner.nextLine();

            Map<String, String> association = current.getAssociation() != null ? new HashMap<>(current.getAssociation()) : new HashMap<>();

            System.out.println("Current association: " + association);
            System.out.print("Add/Replace association principal (leave empty to keep current): ");
            String principal = scanner.nextLine().trim();
            if (!principal.isEmpty()) {
                System.out.print("Privilege Set ID for '" + principal + "': ");
                String privilegeSetId = scanner.nextLine().trim();
                if (privilegeSetId.isEmpty()) {
                    System.out.println("PrivilegeSetId cannot be empty.");
                    return;
                }
                association.put(principal, privilegeSetId);
            }

            if (association.isEmpty()) {
                System.out.println("ACL must include at least one association.");
                return;
            }

            AccessControlListRequest request = new AccessControlListRequest();
            request.setName(name.isEmpty() ? current.getName() : name);
            request.setDescription(description.isEmpty() ? current.getDescription() : description);
            request.setAssociation(association);

            AccessControlListResponse updated = client.updateAcl(id, request);
            System.out.println("ACL updated successfully!");
            System.out.println("ID: " + updated.getId());
            System.out.println("Name: " + updated.getName());
            System.out.println("Association: " + updated.getAssociation());
        } catch (Exception e) {
            System.out.println("Failed to update ACL: " + e.getMessage());
        }
    }

    private void deleteAcl() {
        try {
            System.out.print("ACL ID: ");
            String id = scanner.nextLine().trim();
            client.deleteAcl(id);
            System.out.println("ACL deleted successfully!");
        } catch (Exception e) {
            System.out.println("Failed to delete ACL: " + e.getMessage());
        }
    }

    private void getDocumentClassInfo() {
        System.out.println("\n--- Document Class Info menu ---");
        System.out.println("1. By ID");
        System.out.println("2. By name");
        System.out.println("3. Back");
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
                break;
            default:
                System.out.println("Invalid option!");
        }

    }

    private void showAuthenticationManagementMenu() {
        while (true) {
            System.out.println("\n--- Authentication Management ---");
            System.out.println("1. List Users");
            System.out.println("2. List Groups");
            System.out.println("3. Create Group");
            System.out.println("4. Add Users To Group");
            System.out.println("5. Update User Privilege Set");
            System.out.println("6. Back");
            System.out.print("Choose option: ");

            int choice = getIntInput();
            switch (choice) {
                case 1:
                    listUsers();
                    break;
                case 2:
                    listGroups();
                    break;
                case 3:
                    createGroup();
                    break;
                case 4:
                    addUsersToGroup();
                    break;
                case 5:
                    updateUserPrivilegeSet();
                    break;
                case 6:
                    return;
                default:
                    System.out.println("Invalid option!");
            }
        }
    }

    private void createGroup() {
        try {
            System.out.print("Group name: ");
            String name = scanner.nextLine().trim();
            if (name.isEmpty()) {
                System.out.println("Group name is required.");
                return;
            }
            System.out.print("Description: ");
            String description = scanner.nextLine().trim();

            GroupRequest request = new GroupRequest();
            request.setName(name);
            request.setDescription(description);
            request.setUserIds(new ArrayList<>());

            GroupResponse created = client.createGroup(request);
            System.out.println("Group created successfully!");
            System.out.println("ID: " + created.getId());
            System.out.println("Name: " + created.getName());
        } catch (Exception e) {
            System.out.println("Failed to create group: " + e.getMessage());
        }
    }

    private void addUsersToGroup() {
        try {
            List<GroupResponse> groups = client.getGroups();
            if (groups == null || groups.isEmpty()) {
                System.out.println("No groups found. Create a group first.");
                return;
            }
            List<UserResponse> users = client.getUsers();
            if (users == null || users.isEmpty()) {
                System.out.println("No users found.");
                return;
            }

            System.out.println("\n--- Groups ---");
            for (int i = 0; i < groups.size(); i++) {
                GroupResponse g = groups.get(i);
                System.out.println((i + 1) + ". " + g.getName() + " (ID: " + g.getId() + ")");
            }
            System.out.print("Choose a group (enter number): ");
            int groupChoice = getIntInput();
            if (groupChoice < 1 || groupChoice > groups.size()) {
                System.out.println("Invalid choice!");
                return;
            }
            GroupResponse selectedGroup = groups.get(groupChoice - 1);

            System.out.println("\n--- Users ---");
            for (int i = 0; i < users.size(); i++) {
                UserResponse u = users.get(i);
                System.out.println((i + 1) + ". " + u.getUsername() + " (ID: " + u.getId() + ")");
            }

            System.out.print("Enter user numbers to add (comma-separated): ");
            String raw = scanner.nextLine().trim();
            if (raw.isEmpty()) {
                System.out.println("No users selected.");
                return;
            }

            List<String> userIdsToAdd = new ArrayList<>();
            for (String part : raw.split(",")) {
                String trimmed = part.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                int idx;
                try {
                    idx = Integer.parseInt(trimmed);
                } catch (NumberFormatException nfe) {
                    System.out.println("Invalid user number: " + trimmed);
                    return;
                }
                if (idx < 1 || idx > users.size()) {
                    System.out.println("User number out of range: " + idx);
                    return;
                }
                userIdsToAdd.add(users.get(idx - 1).getId());
            }

            if (userIdsToAdd.isEmpty()) {
                System.out.println("No users selected.");
                return;
            }

            GroupResponse updated = client.addUsersToGroup(selectedGroup.getId(), userIdsToAdd);
            System.out.println("Users added to group successfully!");
            System.out.println("Group: " + updated.getName() + " (ID: " + updated.getId() + ")");
            System.out.println("Users: " + updated.getUserNames());
        } catch (Exception e) {
            System.out.println("Failed to add users to group: " + e.getMessage());
        }
    }

    private void updateUserPrivilegeSet() {
        try {
            List<UserResponse> users = client.getUsers();
            if (users == null || users.isEmpty()) {
                System.out.println("No users found.");
                return;
            }

            List<PrivilegeSetResponse> privilegeSets = client.getPrivilegeSets();
            if (privilegeSets == null || privilegeSets.isEmpty()) {
                System.out.println("No privilege sets found.");
                return;
            }

            System.out.println("\n--- Users ---");
            for (int i = 0; i < users.size(); i++) {
                UserResponse u = users.get(i);
                System.out.println((i + 1) + ". " + u.getUsername() + " (ID: " + u.getId() + ") | Privilege Set: " + u.getPrivilegeSetName() + " (" + u.getPrivilegeSetId() + ")");
            }

            System.out.print("\nChoose a user (enter number): ");
            int userChoice = getIntInput();
            if (userChoice < 1 || userChoice > users.size()) {
                System.out.println("Invalid choice!");
                return;
            }
            UserResponse selectedUser = users.get(userChoice - 1);

            System.out.println("\n--- Privilege Sets ---");
            for (int i = 0; i < privilegeSets.size(); i++) {
                PrivilegeSetResponse ps = privilegeSets.get(i);
                System.out.println((i + 1) + ". " + ps.getName() + " (ID: " + ps.getId() + ")");
            }

            System.out.print("\nChoose a privilege set (enter number): ");
            int psChoice = getIntInput();
            if (psChoice < 1 || psChoice > privilegeSets.size()) {
                System.out.println("Invalid choice!");
                return;
            }
            PrivilegeSetResponse selectedPs = privilegeSets.get(psChoice - 1);

            UserResponse updated = client.updateUserPrivilegeSet(selectedUser.getId(), selectedPs.getId());
            System.out.println("User privilege set updated successfully!");
            System.out.println("Username: " + updated.getUsername());
            System.out.println("Privilege Set ID: " + updated.getPrivilegeSetId());
            System.out.println("Privilege Set Name: " + updated.getPrivilegeSetName());
        } catch (Exception e) {
            System.out.println("Failed to update user privilege set: " + e.getMessage());
        }
    }

    private void showAuthorizationManagementMenu() {
        while (true) {
            System.out.println("\n--- Authorization Management ---");
            System.out.println("1. Privileges");
            System.out.println("2. Privilege Sets");
            System.out.println("3. ACLs");
            System.out.println("4. Back");
            System.out.print("Choose option: ");

            int choice = getIntInput();
            switch (choice) {
                case 1:
                    showPrivilegesMenu();
                    break;
                case 2:
                    showPrivilegeSetsMenu();
                    break;
                case 3:
                    showAclsMenu();
                    break;
                case 4:
                    return;
                default:
                    System.out.println("Invalid option!");
            }
        }
    }

    private void showPrivilegesMenu() {
        while (true) {
            System.out.println("\n--- Privileges ---");
            System.out.println("1. List Privileges");
            System.out.println("2. Create Privilege");
            System.out.println("3. Delete Privilege");
            System.out.println("4. Back");
            System.out.print("Choose option: ");

            int choice = getIntInput();
            switch (choice) {
                case 1:
                    listPrivileges();
                    break;
                case 2:
                    createPrivilege();
                    break;
                case 3:
                    deletePrivilege();
                    break;
                case 4:
                    return;
                default:
                    System.out.println("Invalid option!");
            }
        }
    }

    private void showPrivilegeSetsMenu() {
        while (true) {
            System.out.println("\n--- Privilege Sets ---");
            System.out.println("1. List Privilege Sets");
            System.out.println("2. Create Privilege Set");
            System.out.println("3. Update Privilege Set");
            System.out.println("4. Delete Privilege Set");
            System.out.println("5. Back");
            System.out.print("Choose option: ");

            int choice = getIntInput();
            switch (choice) {
                case 1:
                    listPrivilegeSets();
                    break;
                case 2:
                    createPrivilegeSet();
                    break;
                case 3:
                    updatePrivilegeSet();
                    break;
                case 4:
                    deletePrivilegeSet();
                    break;
                case 5:
                    return;
                default:
                    System.out.println("Invalid option!");
            }
        }
    }

    private void showAclsMenu() {
        while (true) {
            System.out.println("\n--- Access Control Lists (ACLs) ---");
            System.out.println("1. List ACLs");
            System.out.println("2. Create ACL");
            System.out.println("3. Update ACL");
            System.out.println("4. Delete ACL");
            System.out.println("5. Back");
            System.out.print("Choose option: ");

            int choice = getIntInput();
            switch (choice) {
                case 1:
                    listAcls();
                    break;
                case 2:
                    createAcl();
                    break;
                case 3:
                    updateAcl();
                    break;
                case 4:
                    deleteAcl();
                    break;
                case 5:
                    return;
                default:
                    System.out.println("Invalid option!");
            }
        }
    }

    private void showDataModelManagementMenu() {
        while (true) {
            System.out.println("\n--- Data Model Management ---");
            System.out.println("1. List Document Classes");
            System.out.println("2. Get Single Document Class");
            System.out.println("3. Create Document Class");
            System.out.println("4. Modify Document Class");
            System.out.println("5. Back");
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
                    return;
                default:
                    System.out.println("Invalid option!");
            }
        }
    }

    private void showDocumentManagementMenu() {
        while (true) {
            System.out.println("\n--- Document Management ---");
            System.out.println("1. List Documents");
            System.out.println("2. Create Document");
            System.out.println("3. Delete Document");
            System.out.println("4. Upload attachment to an existing document");
            System.out.println("5. Multipart Upload Attachment (direct to S3)");
            System.out.println("6. Search Documents");
            System.out.println("7. Lock Document");
            System.out.println("8. Renew Lock");
            System.out.println("9. Unlock Document");
            System.out.println("10. Update Document (requires lock)");
            System.out.println("11. Guided Lock Demo (lock->renew->update->upload->unlock)");
            System.out.println("12. Back");
            System.out.print("Choose option: ");

            int choice = getIntInput();
            switch (choice) {
                case 1:
                    listDocuments();
                    break;
                case 2:
                    createDocument();
                    break;
                case 3:
                    deleteDocument();
                    break;
                case 4:
                    uploadAttachmentToExistingDocument();
                    break;
                case 5:
                    multipartUploadAttachmentDemo();
                    break;
                case 6:
                    searchDocuments();
                    break;
                case 7:
                    lockDocument();
                    break;
                case 8:
                    renewLock();
                    break;
                case 9:
                    unlockDocument();
                    break;
                case 10:
                    updateExistingDocument();
                    break;
                case 11:
                    guidedLockDemo();
                    break;
                case 12:
                    return;
                default:
                    System.out.println("Invalid option!");
            }
        }
    }

    private void multipartUploadAttachmentDemo() {
        DocumentLockResponse lock = null;
        String sessionId = null;
        String documentId = null;
        try {
            System.out.print("Document ID: ");
            documentId = scanner.nextLine().trim();
            if (documentId.isEmpty()) {
                System.out.println("Document ID cannot be empty.");
                return;
            }

            System.out.print("Local file path: ");
            String filePath = scanner.nextLine().trim();
            if (filePath.isEmpty()) {
                System.out.println("File path cannot be empty.");
                return;
            }

            File file = new File(filePath);
            if (!file.exists() || !file.isFile()) {
                System.out.println("File not found: " + file.getAbsolutePath());
                return;
            }

            lock = client.lockDocument(documentId, 900);

            String contentType;
            try {
                contentType = Files.probeContentType(file.toPath());
            } catch (Exception ignored) {
                contentType = null;
            }
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream";
            }

            MultipartUploadInitRequest initRequest = new MultipartUploadInitRequest();
            initRequest.setFileName(file.getName());
            initRequest.setContentType(contentType);
            initRequest.setFileSize(file.length());

            MultipartUploadInitResponse initResponse = client.initMultipartUpload(documentId, initRequest, lock.getLockId());
            sessionId = initResponse.getSessionId();

            int partSizeBytes = initResponse.getPartSizeBytes() != null ? initResponse.getPartSizeBytes() : 10 * 1024 * 1024;
            long totalSize = file.length();
            int totalParts = (int) Math.ceil((double) totalSize / (double) partSizeBytes);
            if (totalParts <= 0) {
                System.out.println("Invalid file size.");
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

                    URI presignedUrl = URI.create(presign.getPresignedUrl());
                    System.out.println("Uploading to presigned URL: " + presignedUrl);

                    HttpRequest putRequest = HttpRequest.newBuilder()
                            .uri(presignedUrl)
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

                    System.out.println("Uploaded part " + partNumber + "/" + totalParts + " (" + read + " bytes)");
                }
            }

            if (completedParts.isEmpty()) {
                throw new RuntimeException("No parts uploaded.");
            }

            MultipartUploadCompleteRequest completeRequest = new MultipartUploadCompleteRequest();
            completeRequest.setParts(completedParts);

            MultipartUploadCompleteResponse completeResponse = client.completeMultipartUpload(documentId, sessionId, completeRequest, lock.getLockId());
            System.out.println("Multipart upload completed!");
            System.out.println("Attachment ID: " + completeResponse.getAttachmentId());

            try {
                String downloadUrl = client.getAttachmentDownloadUrl(documentId, completeResponse.getAttachmentId());
                System.out.println("Download URL: " + downloadUrl);
            } catch (Exception e) {
                System.out.println("Failed to get download URL: " + e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("Multipart upload failed: " + e.getMessage());
            if (documentId != null && sessionId != null && lock != null) {
                try {
                    client.abortMultipartUpload(documentId, sessionId, lock.getLockId());
                    System.out.println("Multipart session aborted.");
                } catch (Exception ignored) {
                }
            }
        } finally {
            if (documentId != null && lock != null) {
                try {
                    client.unlockDocument(documentId, lock.getLockId());
                } catch (Exception ignored) {
                }
            }
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
        System.out.println("1. Document Management");
        System.out.println("2. List Tenants");
        System.out.println("3. Logout");
        System.out.println("4. Exit");
        System.out.print("Choose option: ");

        int choice = getIntInput();

        switch (choice) {
            case 1:
                showDocumentManagementMenu();
                break;
            case 2:
                listTenantsTopLevel();
                break;
            case 3:
                logout();
                break;
            case 4:
                System.out.println("Goodbye!");
                System.exit(0);
                break;
            default:
                System.out.println("Invalid option!");
        }
    }

    private void listTenantsTopLevel() {
        try {
            List<TenantResponse> tenants = client.listTenants();
            System.out.println("\n--- Tenants (" + (tenants != null ? tenants.size() : 0) + ") ---");
            if (tenants == null || tenants.isEmpty()) {
                return;
            }

            for (int i = 0; i < tenants.size(); i++) {
                TenantResponse t = tenants.get(i);
                System.out.println((i + 1) + ". " + t.getTenantKey() + " (enabled=" + t.isEnabled() + ")");
            }

            System.out.println("Current active tenant: " + client.getActiveTenant());
            System.out.print("Switch active tenant? Enter number (or 0 to keep current): ");
            int choice = getIntInput();
            if (choice >= 1 && choice <= tenants.size()) {
                TenantResponse selected = tenants.get(choice - 1);
                client.setActiveTenant(selected.getTenantKey());
                System.out.println("Active tenant set to: " + client.getActiveTenant());
            }
        } catch (Exception e) {
            System.out.println("Failed to list tenants: " + e.getMessage());
        }
    }

    private void login() {
        System.out.println("Tenant (default: " + client.getActiveTenant() + "): ");
        String tenant = scanner.nextLine().trim();
        if (tenant.isEmpty()) {
            tenant = client.getActiveTenant();
        }
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        try {
            LoginRequest request = new LoginRequest();
            request.setUsername(username);
            request.setPassword(password);

            LoginResponse response = client.login(tenant, request);
            currentUser = response;
            client.setActiveTenant(tenant);
            client.setAuthToken(tenant, response.getToken());

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
        lastLockedDocumentId = null;
        lastLockId = null;
        System.out.println("Logged out successfully!");
    }

    private void lockDocument() {
        try {
            String documentId = promptDocumentId();
            System.out.print("Lease seconds (default 900): ");
            String leaseInput = scanner.nextLine().trim();
            Integer leaseSeconds = leaseInput.isEmpty() ? 900 : Integer.parseInt(leaseInput);

            DocumentLockResponse lock = client.lockDocument(documentId, leaseSeconds);
            lastLockedDocumentId = documentId;
            lastLockId = lock.getLockId();

            System.out.println("Lock acquired:");
            System.out.println("- documentId=" + lock.getDocumentId());
            System.out.println("- lockedBy=" + lock.getLockedBy());
            System.out.println("- lockId=" + lock.getLockId());
            System.out.println("- expiresAt=" + lock.getLockExpiresAt());
        } catch (Exception e) {
            System.out.println("Failed to lock document: " + e.getMessage());
        }
    }

    private void renewLock() {
        try {
            String documentId = promptDocumentIdWithDefault(lastLockedDocumentId);
            String lockId = promptLockIdWithDefault(lastLockId);

            System.out.print("New lease seconds (default 900): ");
            String leaseInput = scanner.nextLine().trim();
            Integer leaseSeconds = leaseInput.isEmpty() ? 900 : Integer.parseInt(leaseInput);

            DocumentLockResponse renewed = client.renewLock(documentId, lockId, leaseSeconds);
            lastLockedDocumentId = documentId;
            lastLockId = renewed.getLockId();

            System.out.println("Lock renewed:");
            System.out.println("- documentId=" + renewed.getDocumentId());
            System.out.println("- lockedBy=" + renewed.getLockedBy());
            System.out.println("- lockId=" + renewed.getLockId());
            System.out.println("- expiresAt=" + renewed.getLockExpiresAt());
        } catch (Exception e) {
            System.out.println("Failed to renew lock: " + e.getMessage());
        }
    }

    private void unlockDocument() {
        try {
            String documentId = promptDocumentIdWithDefault(lastLockedDocumentId);
            String lockId = promptLockIdWithDefault(lastLockId);

            client.unlockDocument(documentId, lockId);
            System.out.println("Unlocked document: " + documentId);

            if (documentId != null && documentId.equals(lastLockedDocumentId)) {
                lastLockedDocumentId = null;
                lastLockId = null;
            }
        } catch (Exception e) {
            System.out.println("Failed to unlock document: " + e.getMessage());
        }
    }

    private void updateExistingDocument() {
        try {
            String documentId = promptDocumentIdWithDefault(lastLockedDocumentId);
            String lockId = promptLockIdWithDefault(lastLockId);

            DocumentResponse current = client.getDocument(documentId);
            System.out.println("Current document:");
            printDocument(current);

            System.out.print("Attribute key to update: ");
            String key = scanner.nextLine().trim();
            if (key.isEmpty()) {
                System.out.println("No key provided. Update cancelled.");
                return;
            }
            System.out.print("New value: ");
            String value = scanner.nextLine();

            DocumentRequest request = new DocumentRequest();
            request.setDocumentClassId(current.getDocumentClassId());
            request.setAccessControlListId(current.getAccessControlListId());
            if (current.getAttributes() != null) {
                request.getAttributes().putAll(current.getAttributes());
            }
            request.getAttributes().put(key, value);

            DocumentResponse updated = client.updateDocument(documentId, request, lockId);
            System.out.println("Updated document:");
            printDocument(updated);
        } catch (Exception e) {
            System.out.println("Failed to update document: " + e.getMessage());
        }
    }

    private void guidedLockDemo() {
        try {
            String documentId = promptDocumentId();
            System.out.println("\n--- Step 1: Acquire lock ---");
            DocumentLockResponse lock = client.lockDocument(documentId, 120);
            lastLockedDocumentId = documentId;
            lastLockId = lock.getLockId();
            System.out.println("LockId=" + lock.getLockId() + " expiresAt=" + lock.getLockExpiresAt());

            System.out.println("\n--- Step 2: Renew lock ---");
            DocumentLockResponse renewed = client.renewLock(documentId, lock.getLockId(), 300);
            lastLockId = renewed.getLockId();
            System.out.println("Renewed expiresAt=" + renewed.getLockExpiresAt());

            System.out.println("\n--- Step 3: Update (requires lock) ---");
            DocumentResponse current = client.getDocument(documentId);
            DocumentRequest req = new DocumentRequest();
            req.setDocumentClassId(current.getDocumentClassId());
            req.setAccessControlListId(current.getAccessControlListId());
            if (current.getAttributes() != null) {
                req.getAttributes().putAll(current.getAttributes());
            }
            req.getAttributes().put("demoUpdatedAt", String.valueOf(System.currentTimeMillis()));
            DocumentResponse updated = client.updateDocument(documentId, req, renewed.getLockId());
            System.out.println("Updated attribute demoUpdatedAt=" + updated.getAttributes().get("demoUpdatedAt"));

            System.out.println("\n--- Step 4: Upload attachment (requires lock) ---");
            System.out.print("Enter file path to upload (or empty to skip): ");
            String filePath = scanner.nextLine().trim();
            if (!filePath.isEmpty()) {
                File file = new File(filePath);
                if (!file.exists() || !file.isFile()) {
                    System.out.println("File not found, skipping upload: " + filePath);
                } else {
                    List<File> files = new ArrayList<>();
                    files.add(file);
                    List<DocumentAttachmentDto> uploaded = client.uploadAttachments(documentId, files, renewed.getLockId());
                    System.out.println("Uploaded attachments: " + uploaded.size());
                }
            } else {
                System.out.println("Skipping upload.");
            }

            System.out.println("\n--- Step 5: Unlock ---");
            client.unlockDocument(documentId, renewed.getLockId());
            lastLockedDocumentId = null;
            lastLockId = null;
            System.out.println("Unlocked.");
        } catch (Exception e) {
            System.out.println("Guided demo failed: " + e.getMessage());
        }
    }

    private String promptDocumentId() {
        System.out.print("Document ID: ");
        return scanner.nextLine().trim();
    }

    private String promptDocumentIdWithDefault(String defaultId) {
        if (defaultId != null && !defaultId.isBlank()) {
            System.out.print("Document ID (default " + defaultId + "): ");
            String input = scanner.nextLine().trim();
            return input.isEmpty() ? defaultId : input;
        }
        return promptDocumentId();
    }

    private String promptLockIdWithDefault(String defaultLockId) {
        if (defaultLockId != null && !defaultLockId.isBlank()) {
            System.out.print("Lock ID (default " + defaultLockId + "): ");
            String input = scanner.nextLine().trim();
            return input.isEmpty() ? defaultLockId : input;
        }
        System.out.print("Lock ID: ");
        return scanner.nextLine().trim();
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

            DocumentClassRequest request = getDocumentClassRequest(current, displayName, description);

            DocumentClassResponse response = client.updateDocumentClass(id, request);
            System.out.println("Document class updated successfully: " + response.getName());

        } catch (Exception e) {
            System.out.println("Failed to modify document class: " + e.getMessage());
        }
    }

    private static @NonNull DocumentClassRequest getDocumentClassRequest(DocumentClassResponse current, String displayName, String description) {
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
        return request;
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
                System.out.println("Users: " + (group.getUserNames() != null ? group.getUserNames() : group.getUserIds()));
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
            List<DocumentClassResponse> classes = client.getDocumentClasses();
            if (classes == null || classes.isEmpty()) {
                System.out.println("No document classes found.");
                return;
            }

            System.out.println("\n--- Document Classes ---");
            for (int i = 0; i < classes.size(); i++) {
                DocumentClassResponse dc = classes.get(i);
                System.out.println((i + 1) + ". " + dc.getName() + " (ID: " + dc.getId() + ")");
            }

            System.out.print("\nChoose a document class (enter number): ");
            int classChoice = getIntInput();
            if (classChoice < 1 || classChoice > classes.size()) {
                System.out.println("Invalid choice!");
                return;
            }

            DocumentClassResponse selectedClass = classes.get(classChoice - 1);
            List<DocumentResponse> documents = client.searchDocuments(selectedClass.getId(), Map.of());
            System.out.println("\n--- Documents for class '" + selectedClass.getName() + "' (" + documents.size() + ") ---");

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
                            System.out.println("  Type: " + attachment.getFileType());
                            
                            // Get presigned URL for this attachment
                            try {
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
        
        // Display attachment URLs if available
        if (doc.getDocuments() != null && !doc.getDocuments().isEmpty()) {
            System.out.println("--- Attachments ---");
            for (var attachment : doc.getDocuments()) {
                System.out.println("    File: " + attachment.getFileName());
                System.out.println("    Type: " + attachment.getFileType());
                System.out.println("    Size: " + attachment.getFileSize() + " bytes");
                if (attachment.getPresignedUrl() != null) {
                    System.out.println("    URL: " + attachment.getPresignedUrl());
                    if (attachment.getPresignedUrlExpiresAt() != null) {
                        System.out.println("    Expires: " + attachment.getPresignedUrlExpiresAt());
                    }
                } else {
                    System.out.println("    URL: No presigned URL available");
                }
                System.out.println();
            }
        }
    }

    private void createDocument() {
        try {
            createGenericDocument();
        } catch (Exception e) {
            System.out.println("Failed to create document: " + e.getMessage());
        }
    }
    
    private void createGenericDocument() {
        try {
            List<DocumentClassResponse> classes = client.getDocumentClasses();
            if (classes == null || classes.isEmpty()) {
                System.out.println("No document classes found. Create a document class first.");
                return;
            }

            System.out.println("\n--- Document Classes ---");
            for (int i = 0; i < classes.size(); i++) {
                DocumentClassResponse dc = classes.get(i);
                System.out.println((i + 1) + ". " + dc.getName() + " (ID: " + dc.getId() + ")");
            }

            System.out.print("\nChoose a document class (enter number): ");
            int classChoice = getIntInput();
            if (classChoice < 1 || classChoice > classes.size()) {
                System.out.println("Invalid choice!");
                return;
            }

            DocumentClassResponse selectedClass = classes.get(classChoice - 1);
            String classId = selectedClass.getId();

            // find document class by id (ensures we have full attribute metadata)
            DocumentClassResponse docClass = client.getDocumentClassById(classId);

            DocumentRequest request = new DocumentRequest();
            request.setDocumentClassId(classId);

            try {
                List<AccessControlListResponse> acls = client.getAcls();
                if (acls == null || acls.isEmpty()) {
                    if (docClass.getAclId() == null || docClass.getAclId().isBlank()) {
                        System.out.println("No ACLs found. Create an ACL first (per-document ACL may be required).");
                        return;
                    }
                    System.out.println("No ACLs found; proceeding with document class ACL (ID: " + docClass.getAclId() + ")");
                } else {
                    System.out.println("\n--- ACLs ---");
                    if (docClass.getAclId() != null && !docClass.getAclId().isBlank()) {
                        System.out.println("0. Use document class ACL (ID: " + docClass.getAclId() + ")");
                    }
                    for (int i = 0; i < acls.size(); i++) {
                        AccessControlListResponse acl = acls.get(i);
                        System.out.println((i + 1) + ". " + acl.getName() + " (ID: " + acl.getId() + ")");
                    }

                    int minChoice = (docClass.getAclId() != null && !docClass.getAclId().isBlank()) ? 0 : 1;
                    System.out.print("\nChoose an ACL (enter number): ");
                    int aclChoice = getIntInput();
                    if (aclChoice < minChoice || aclChoice > acls.size()) {
                        System.out.println("Invalid choice!");
                        return;
                    }

                    if (aclChoice != 0) {
                        AccessControlListResponse selectedAcl = acls.get(aclChoice - 1);
                        request.setAccessControlListId(selectedAcl.getId());
                    }
                }
            } catch (Exception e) {
                System.out.println("Failed to list ACLs for selection: " + e.getMessage());
                if (docClass.getAclId() == null || docClass.getAclId().isBlank()) {
                    System.out.print("Enter ACL ID to use for this document (required), or leave blank to cancel: ");
                    String aclId = scanner.nextLine().trim();
                    if (aclId.isEmpty()) {
                        return;
                    }
                    request.setAccessControlListId(aclId);
                } else {
                    System.out.println("Proceeding with document class ACL (ID: " + docClass.getAclId() + ")");
                }
            }

            System.out.println("\n--- Document attribute entry ---  ");
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
    
    private void deleteDocument() {
        try {
            System.out.print("Document ID: ");
            String id = scanner.nextLine();

            DocumentLockResponse lock = client.lockDocument(id, 300);
            try {
                client.deleteDocument(id, lock.getLockId());
                System.out.println("Document deleted successfully!");
            } finally {
                try {
                    client.unlockDocument(id, lock.getLockId());
                } catch (Exception ignored) {
                }
            }

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

            DocumentLockResponse lock = client.lockDocument(documentId, 900);
            
            try {
                if (attachmentFiles.size() == 1) {
                    // Single file upload
                    java.io.File file = attachmentFiles.get(0);
                    List<File> files = new ArrayList<>();
                    files.add(file);
                    List<DocumentAttachmentDto> response = client.uploadAttachments(documentId, files, lock.getLockId());
                    System.out.println("Attachment uploaded successfully!");
                    System.out.println("Response: " + response);
                } else {
                    // Multiple files upload
                    List<DocumentAttachmentDto> response = client.uploadAttachments(documentId, attachmentFiles, lock.getLockId());
                    System.out.println("Multiple attachments uploaded successfully!");
                    System.out.println("Response: " + response);
                }
            } finally {
                try {
                    client.unlockDocument(documentId, lock.getLockId());
                } catch (Exception ignored) {
                }
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
        try {
            System.out.println("\n--- Search Documents ---");
            
            // Step 1: List all document classes and allow user to choose one
            List<DocumentClassResponse> documentClasses = client.getDocumentClasses();
            if (documentClasses.isEmpty()) {
                System.out.println("No document classes available. Please create a document class first.");
                return;
            }
            
            System.out.println("Available Document Classes:");
            for (int i = 0; i < documentClasses.size(); i++) {
                System.out.println((i + 1) + ". " + documentClasses.get(i).getName() + 
                                 " (ID: " + documentClasses.get(i).getId() + ")");
            }
            
            System.out.print("Choose a document class (enter number): ");
            int classChoice = getIntInput();
            if (classChoice < 1 || classChoice > documentClasses.size()) {
                System.out.println("Invalid choice!");
                return;
            }
            
            DocumentClassResponse selectedClass = documentClasses.get(classChoice - 1);
            System.out.println("Selected class: " + selectedClass.getName());
            
            // Step 2: List all attribute display names of the selected document class
            if (selectedClass.getAttributes().isEmpty()) {
                System.out.println("This document class has no attributes defined.");
                return;
            }
            
            System.out.println("\nAvailable Attributes:");
            for (int i = 0; i < selectedClass.getAttributes().size(); i++) {
                var attr = selectedClass.getAttributes().get(i);
                System.out.println((i + 1) + ". " + attr.getDisplayName() + 
                                 " (Type: " + attr.getType() + ", Required: " + attr.isRequired() + ")");
            }
            
            // Step 3: Ask user to provide values for attributes
            Map<String, Object> attributeFilters = new java.util.HashMap<>();
            
            System.out.println("\nEnter filter values for attributes (press Enter to skip an attribute):");
            for (var attr : selectedClass.getAttributes()) {
                System.out.print(attr.getDisplayName() + " (" + attr.getType() + "): ");
                String input = scanner.nextLine().trim();
                
                if (!input.isEmpty()) {
                    // Convert input based on attribute type
                    Object value = convertAttributeInput(input, attr.getType());
                    if (value != null) {
                        attributeFilters.put(attr.getId(), value);
                    }
                }
            }
            
            // Step 4: Perform search
            System.out.println("\nSearching documents...");
            
            List<DocumentResponse> results = client.searchDocuments(
                selectedClass.getId(), 
                attributeFilters
            );
            
            // Step 5: Display results
            if (results.isEmpty()) {
                System.out.println("No documents found matching your criteria.");
            } else {
                System.out.println("Found " + results.size() + " documents:");
                System.out.println();
                
                for (DocumentResponse doc : results) {
                    printDocument(doc);
                    System.out.println("---");
                }
            }
            
        } catch (Exception e) {
            System.out.println("Failed to search documents: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private Object convertAttributeInput(String input, String type) {
        try {
            switch (type.toLowerCase()) {
                case "string":
                case "text":
                    return input;
                case "number":
                case "integer":
                case "long":
                    return Long.parseLong(input);
                case "decimal":
                case "double":
                case "float":
                    return Double.parseDouble(input);
                case "boolean":
                    return Boolean.parseBoolean(input);
                case "date":
                    return java.time.LocalDate.parse(input);
                case "datetime":
                    return java.time.LocalDateTime.parse(input);
                default:
                    System.out.println("Unsupported attribute type: " + type + ", using string value");
                    return input;
            }
        } catch (Exception e) {
            System.out.println("Invalid input for type " + type + ": " + input);
            return null;
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
