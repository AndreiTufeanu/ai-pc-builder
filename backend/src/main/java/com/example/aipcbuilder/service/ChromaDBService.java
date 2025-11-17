package com.example.aipcbuilder.service;

import com.example.aipcbuilder.model.PcComponent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class ChromaDBService {

    @Value("${chromadb.server.url:http://localhost:8000}")
    private String chromaDbServerUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ChromaDBService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    // Component data methods
    public void syncComponent(PcComponent component) {
        try {
            Map<String, Object> componentData = createComponentData(component);
            List<Map<String, Object>> componentsList = Collections.singletonList(componentData);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<List<Map<String, Object>>> request =
                    new HttpEntity<>(componentsList, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    chromaDbServerUrl + "/components/upsert",
                    request,
                    String.class
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                System.err.println("Failed to sync component to ChromaDB: " + response.getBody());
            }
        } catch (Exception e) {
            System.err.println("Error syncing component to ChromaDB: " + e.getMessage());
        }
    }

    public void syncAllComponents(List<PcComponent> components) {
        try {
            List<Map<String, Object>> componentsData = new ArrayList<>();
            for (PcComponent component : components) {
                componentsData.add(createComponentData(component));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<List<Map<String, Object>>> request =
                    new HttpEntity<>(componentsData, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    chromaDbServerUrl + "/components/upsert",
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("✓ Synced " + components.size() + " components to ChromaDB");
            } else {
                System.err.println("✗ Failed to sync components: " + response.getBody());
            }

        } catch (Exception e) {
            System.err.println("✗ Error syncing all components to ChromaDB: " + e.getMessage());
            e.printStackTrace(); // Add stack trace for debugging
        }
    }

    public void deleteComponent(Long componentId) {
        try {
            restTemplate.delete(chromaDbServerUrl + "/components/" + componentId);
        } catch (Exception e) {
            System.err.println("Error deleting component from ChromaDB: " + e.getMessage());
        }
    }

    private Map<String, Object> createComponentData(PcComponent component) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", component.getId().toString());
        data.put("name", component.getName());
        data.put("type", component.getType().toString());
        data.put("description", component.getDescription());
        data.put("manufacturer", component.getManufacturer());
        data.put("model", component.getModel());
        data.put("price", component.getPrice() != null ? component.getPrice().doubleValue() : null);
        data.put("specifications", component.getSpecifications());
        return data;
    }

    // Admin knowledge methods
    public void addAdminKnowledge(String content, String knowledgeType, Map<String, Object> metadata) {
        try {
            Map<String, Object> knowledgeData = new HashMap<>();
            knowledgeData.put("content", content);
            knowledgeData.put("knowledge_type", knowledgeType);
            knowledgeData.put("metadata", metadata != null ? metadata : new HashMap<>());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request =
                    new HttpEntity<>(knowledgeData, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    chromaDbServerUrl + "/admin/knowledge",
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("Admin knowledge added to ChromaDB");
            }
        } catch (Exception e) {
            System.err.println("Error adding admin knowledge to ChromaDB: " + e.getMessage());
        }
    }

    // Search methods
    public List<Map<String, Object>> searchComponents(String query, int nResults) {
        return searchCollection("components", query, nResults);
    }

    public List<Map<String, Object>> searchAdminKnowledge(String query, int nResults) {
        return searchCollection("admin_knowledge", query, nResults);
    }

    private List<Map<String, Object>> searchCollection(String collection, String query, int nResults) {
        try {
            Map<String, Object> searchRequest = new HashMap<>();
            searchRequest.put("query", query);
            searchRequest.put("collection", collection);
            searchRequest.put("n_results", nResults);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request =
                    new HttpEntity<>(searchRequest, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    chromaDbServerUrl + "/search",
                    request,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (List<Map<String, Object>>) response.getBody().get("results");
            }
        } catch (Exception e) {
            System.err.println("Error searching ChromaDB: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    public String getChromaDbStatus() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    chromaDbServerUrl,
                    String.class
            );
            return response.getStatusCode() == HttpStatus.OK ? "CONNECTED" : "ERROR";
        } catch (Exception e) {
            return "DISCONNECTED";
        }
    }
}