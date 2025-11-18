package com.example.aipcbuilder.service;

import com.example.aipcbuilder.model.ChatMessage;
import com.example.aipcbuilder.model.PcComponent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

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

    // User messages methods
    public void syncUserMessages(List<ChatMessage> messages) {
        try {
            List<Map<String, Object>> messageData = new ArrayList<>();
            for (ChatMessage message : messages) {
                messageData.add(createUserMessageData(message));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<List<Map<String, Object>>> request =
                    new HttpEntity<>(messageData, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    chromaDbServerUrl + "/user_messages/upsert",
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("✓ Synced " + messages.size() + " user messages to ChromaDB");
            } else {
                System.err.println("✗ Failed to sync user messages: " + response.getBody());
            }
        } catch (Exception e) {
            System.err.println("✗ Error syncing user messages to ChromaDB: " + e.getMessage());
        }
    }

    public void syncLatestUserMessages(Long userId, List<ChatMessage> messages) {
        // Only sync the last 50 messages for this user
        List<ChatMessage> latestMessages = messages.stream()
                .sorted((m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt())) // newest first
                .limit(50)
                .collect(Collectors.toList());

        syncUserMessages(latestMessages);
    }

    private Map<String, Object> createUserMessageData(ChatMessage message) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", message.getId().toString());
        data.put("user_id", message.getUserId().toString());
        data.put("user_message", message.getUserMessage());
        data.put("ai_response", message.getAiResponse());
        data.put("created_at", message.getCreatedAt().toString());
        return data;
    }

    // Search user messages for context
    public List<Map<String, Object>> searchUserMessages(String query, int nResults) {
        return searchCollection("user_messages", query, nResults);
    }

    public List<Map<String, Object>> searchUserMessagesByUser(String query, Long userId, int nResults) {
        try {
            // First search in user_messages collection
            List<Map<String, Object>> results = searchUserMessages(query, nResults * 2); // Get more to filter

            // Filter by user_id
            return results.stream()
                    .filter(result -> {
                        Map<String, Object> metadata = (Map<String, Object>) result.get("metadata");
                        String resultUserId = (String) metadata.get("user_id");
                        return userId.toString().equals(resultUserId);
                    })
                    .limit(nResults)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error searching user messages by user: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Startup cleanup
    public void performStartupCleanup() {
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    chromaDbServerUrl + "/cleanup/startup",
                    null,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("✓ ChromaDB startup cleanup completed");
            } else {
                System.err.println("✗ ChromaDB startup cleanup failed: " + response.getBody());
            }
        } catch (Exception e) {
            System.err.println("✗ Error during ChromaDB startup cleanup: " + e.getMessage());
        }
    }
}