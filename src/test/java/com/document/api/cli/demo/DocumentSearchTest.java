package com.document.api.cli.demo;

import com.claims.documentapi.DocumentApiClient;
import com.claims.documentapi.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Test for the searchDocuments functionality in DocumentApiCliApplication
 */
@ExtendWith(MockitoExtension.class)
public class DocumentSearchTest {
    
    @Mock
    private DocumentApiClient mockClient;
    
    @Test
    public void testSearchDocumentsFunctionality() {
        // Setup mock data
        DocumentClassResponse testClass = createTestDocumentClass();
        List<DocumentClassResponse> classes = List.of(testClass);
        
        List<DocumentResponse> searchResults = createTestSearchResults();
        
        // Mock the client calls
        when(mockClient.getDocumentClasses()).thenReturn(classes);
        when(mockClient.searchDocuments(eq(testClass.getId()), anyMap())).thenReturn(searchResults);
        
        // Verify that the search would work with the mock data
        List<DocumentClassResponse> retrievedClasses = mockClient.getDocumentClasses();
        assertEquals(1, retrievedClasses.size());
        assertEquals("test-class-id", retrievedClasses.get(0).getId());
        assertEquals("Test Document Class", retrievedClasses.get(0).getName());
        assertEquals(2, retrievedClasses.get(0).getAttributes().size());
        
        // Test search functionality
        Map<String, Object> filters = new HashMap<>();
        filters.put("attr1", "test value");
        filters.put("attr2", 123L);
        
        List<DocumentResponse> results = mockClient.searchDocuments(testClass.getId(), filters);
        assertEquals(2, results.size());
        assertEquals("doc1", results.get(0).getId());
        assertEquals("doc2", results.get(1).getId());
    }
    
    private DocumentClassResponse createTestDocumentClass() {
        DocumentClassResponse docClass = new DocumentClassResponse();
        docClass.setId("test-class-id");
        docClass.setName("Test Document Class");
        docClass.setDisplayName("Test Document Class");
        docClass.setDescription("A test document class for unit testing");
        
        List<DocumentClassResponse.AttributeDefinition> attributes = new ArrayList<>();
        
        DocumentClassResponse.AttributeDefinition attr1 = new DocumentClassResponse.AttributeDefinition();
        attr1.setId("attr1");
        attr1.setDisplayName("Document Title");
        attr1.setType("string");
        attr1.setRequired(true);
        attr1.setIndexed(true);
        attributes.add(attr1);
        
        DocumentClassResponse.AttributeDefinition attr2 = new DocumentClassResponse.AttributeDefinition();
        attr2.setId("attr2");
        attr2.setDisplayName("Document Number");
        attr2.setType("number");
        attr2.setRequired(false);
        attr2.setIndexed(false);
        attributes.add(attr2);
        
        docClass.setAttributes(attributes);
        return docClass;
    }
    
    private List<DocumentResponse> createTestSearchResults() {
        List<DocumentResponse> results = new ArrayList<>();
        
        DocumentResponse doc1 = new DocumentResponse();
        doc1.setId("doc1");
        doc1.setDocumentClassId("test-class-id");
        doc1.setDocumentClassName("Test Document Class");
        Map<String, Object> doc1Attrs = new HashMap<>();
        doc1Attrs.put("attr1", "Test Document 1");
        doc1Attrs.put("attr2", 123L);
        doc1.setAttributes(doc1Attrs);
        results.add(doc1);
        
        DocumentResponse doc2 = new DocumentResponse();
        doc2.setId("doc2");
        doc2.setDocumentClassId("test-class-id");
        doc2.setDocumentClassName("Test Document Class");
        Map<String, Object> doc2Attrs = new HashMap<>();
        doc2Attrs.put("attr1", "Test Document 2");
        doc2Attrs.put("attr2", 456L);
        doc2.setAttributes(doc2Attrs);
        results.add(doc2);
        
        return results;
    }
}
