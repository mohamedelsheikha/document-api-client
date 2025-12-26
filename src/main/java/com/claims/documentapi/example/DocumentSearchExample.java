package com.claims.documentapi.example;

import com.claims.documentapi.DocumentApiClient;
import com.claims.documentapi.dto.DocumentSearchRequest;
import com.claims.documentapi.dto.DocumentResponse;
import com.claims.documentapi.dto.LoginRequest;
import com.claims.documentapi.dto.LoginResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Example usage of DocumentApiClient search functionality
 */
public class DocumentSearchExample {
    
    public static void main(String[] args) {
        // Initialize the client
        DocumentApiClient client = new DocumentApiClient("http://localhost:5000");
        
        // Authenticate
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("AdminPass123!");
        
        LoginResponse loginResponse = client.login(loginRequest);
        client.setAuthToken(loginResponse.getToken());
        
        // Example 1: Search with full DocumentSearchRequest object
        DocumentSearchRequest searchRequest = new DocumentSearchRequest();
        searchRequest.setDocumentClassId("6940a0042ab7471239c23acd");
        searchRequest.setPage(0);
        searchRequest.setSize(10);
        searchRequest.setSortBy("createdAt");
        searchRequest.setSortDirection("desc");
        
        // Add attribute filters
        Map<String, Object> filters = new HashMap<>();
        filters.put("status", "ACTIVE");
        filters.put("priority", "HIGH");
        searchRequest.setAttributeFilters(filters);
        
        List<DocumentResponse> results1 = client.searchDocuments(searchRequest);
        System.out.println("Found " + results1.size() + " documents using full search request");
        
        // Example 2: Search with simplified parameters
        Map<String, Object> simpleFilters = new HashMap<>();
        simpleFilters.put("category", "CLAIMS");
        
        List<DocumentResponse> results2 = client.searchDocuments("your-document-class-id", simpleFilters);
        System.out.println("Found " + results2.size() + " documents using simplified search");
        
        // Example 3: Search with pagination and sorting
        List<DocumentResponse> results3 = client.searchDocuments(
            "your-document-class-id", 
            simpleFilters, 
            0,  // page
            25, // size
            "name", 
            "asc"
        );
        System.out.println("Found " + results3.size() + " documents with pagination");
        
        // Display results
        for (DocumentResponse doc : results1) {
            System.out.println("Document ID: " + doc.getId());
            System.out.println("Class: " + doc.getDocumentClassId());
            System.out.println("Created: " + doc.getCreatedAt());
            System.out.println("---");
        }
    }
}
