package com.example.aipcbuilder.service;

import com.example.aipcbuilder.model.ChatMessage;
import com.example.aipcbuilder.model.PcComponent;
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

    private final RestTemplate restTemplate = new RestTemplate();

    // Component data methods
    public void syncComponent(PcComponent component) {
        try {
            Map<String, Object> componentData = createComponentData(component);
            syncComponents(Collections.singletonList(componentData));
        } catch (Exception e) {
            System.err.println("Error syncing component to ChromaDB: " + e.getMessage());
        }
    }

    public void syncAllComponents(List<PcComponent> components) {
        try {
            List<Map<String, Object>> componentsData = components.stream()
                    .map(this::createComponentData)
                    .collect(Collectors.toList());

            syncComponents(componentsData);
        } catch (Exception e) {
            System.err.println("Error syncing all components to ChromaDB: " + e.getMessage());
        }
    }

    private void syncComponents(List<Map<String, Object>> componentsData) {
        HttpEntity<List<Map<String, Object>>> request = createRequest(componentsData);

        ResponseEntity<String> response = restTemplate.postForEntity(
                chromaDbServerUrl + "/components/upsert",
                request,
                String.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            System.out.println("✓ Synced " + componentsData.size() + " components to ChromaDB");
        } else {
            System.err.println("✗ Failed to sync components: " + response.getBody());
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

            HttpEntity<Map<String, Object>> request = createRequest(knowledgeData);

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

    public List<Map<String, Object>> searchUserMessages(String query, int nResults) {
        return searchCollection("user_messages", query, nResults);
    }

    private List<Map<String, Object>> searchCollection(String collection, String query, int nResults) {
        try {
            Map<String, Object> searchRequest = Map.of(
                    "query", query,
                    "collection", collection,
                    "n_results", nResults
            );

            HttpEntity<Map<String, Object>> request = createRequest(searchRequest);

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

    public List<Map<String, Object>> searchUserMessagesByUser(String query, Long userId, int nResults) {
        try {
            List<Map<String, Object>> results = searchUserMessages(query, nResults * 2);

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

    // User messages methods
    public void syncUserMessages(List<ChatMessage> messages) {
        try {
            List<Map<String, Object>> messageData = messages.stream()
                    .map(this::createUserMessageData)
                    .collect(Collectors.toList());

            HttpEntity<List<Map<String, Object>>> request = createRequest(messageData);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    chromaDbServerUrl + "/user_messages/upsert",
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("✓ Synced " + messages.size() + " user messages to ChromaDB");
            }
        } catch (Exception e) {
            System.err.println("Error syncing user messages to ChromaDB: " + e.getMessage());
        }
    }

    public void syncLatestUserMessages(Long userId, List<ChatMessage> messages) {
        List<ChatMessage> latestMessages = messages.stream()
                .sorted((m1, m2) -> m2.getCreatedAt().compareTo(m1.getCreatedAt())) // Fixed: newest first
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
            }
        } catch (Exception e) {
            System.err.println("Error during ChromaDB startup cleanup: " + e.getMessage());
        }
    }

    public String getChromaDbStatus() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(chromaDbServerUrl, String.class);
            return response.getStatusCode() == HttpStatus.OK ? "CONNECTED" : "ERROR";
        } catch (Exception e) {
            return "DISCONNECTED";
        }
    }

    // Helper method to create HTTP requests
    private <T> HttpEntity<T> createRequest(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}