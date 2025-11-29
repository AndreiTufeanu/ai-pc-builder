package com.example.aipcbuilder.service;

import com.example.aipcbuilder.model.ChatMessage;
import com.example.aipcbuilder.model.PcComponent;
import com.example.aipcbuilder.service.helper.ChromaDataHelper;
import com.example.aipcbuilder.utils.HttpHelper;
import io.micrometer.common.lang.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChromaDBService {

    @Value("${chromadb.server.url:http://localhost:8000}")
    private String chromaDbServerUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ChromaDataHelper chromaDataHelper;
    private final HttpHelper httpHelper;

    public void syncComponent(PcComponent component) {
        syncComponentsBatch(Collections.singletonList(component));
    }

    public void syncComponentsBatch(List<PcComponent> components) {
        try {
            List<Map<String, Object>> componentsData = components.stream()
                    .map(chromaDataHelper::createComponentData)
                    .collect(Collectors.toList());

            upsertComponentsToChromaDB(componentsData);
        } catch (Exception e) {
            System.err.println("Error syncing components batch to ChromaDB: " + e.getMessage());
        }
    }

    private void upsertComponentsToChromaDB(List<Map<String, Object>> componentsData) {
        HttpEntity<List<Map<String, Object>>> request = httpHelper.createJsonRequest(componentsData);

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

    // Admin knowledge methods
    public void addAdminKnowledge(String content, String knowledgeType, Map<String, Object> metadata) {
        try {
            Map<String, Object> knowledgeData = new HashMap<>();
            knowledgeData.put("content", content);
            knowledgeData.put("knowledge_type", knowledgeType);
            knowledgeData.put("metadata", metadata != null ? metadata : new HashMap<>());

            HttpEntity<Map<String, Object>> request = httpHelper.createJsonRequest(knowledgeData);

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
    public List<Map<String, Object>> searchComponents(String query, int nResults, @Nullable String componentType) {
        return searchCollection("components", query, nResults, componentType);
    }

    public List<Map<String, Object>> searchAdminKnowledge(String query, int nResults) {
        return searchCollection("admin_knowledge", query, nResults, null);
    }

    public List<Map<String, Object>> searchUserMessages(String query, int nResults) {
        return searchCollection("user_messages", query, nResults, null);
    }

    private List<Map<String, Object>> searchCollection(String collection, String query, int nResults, @Nullable String componentType) {
        try {
            Map<String, Object> searchRequest = new HashMap<>(Map.of(
                    "query", query,
                    "collection", collection,
                    "n_results", nResults
            ));

            if (componentType != null && !componentType.trim().isEmpty()) {
                Map<String, Object> whereFilter = Map.of("component_type", componentType.toUpperCase());
                searchRequest.put("where", whereFilter);
            }

            HttpEntity<Map<String, Object>> request = httpHelper.createJsonRequest(searchRequest);

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
                    .map(chromaDataHelper::createUserMessageData)
                    .collect(Collectors.toList());

            HttpEntity<List<Map<String, Object>>> request = httpHelper.createJsonRequest(messageData);

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
                .sorted((m1, m2) -> m2.getCreatedAt().compareTo(m1.getCreatedAt()))
                .limit(50)
                .collect(Collectors.toList());

        syncUserMessages(latestMessages);
    }

    public void syncAdminKnowledge(List<ChatMessage> adminMessages) {
        try {
            for (ChatMessage message : adminMessages) {
                Map<String, Object> metadata = Map.of(
                        "timestamp", message.getCreatedAt().toString(),
                        "source", "admin_training",
                        "message_id", message.getId().toString(),
                        "admin_user_id", message.getUserId().toString()
                );

                // Use the user message as the knowledge content
                addAdminKnowledge(message.getUserMessage(), "TRAINING", metadata);
            }
            System.out.println("✓ Synced " + adminMessages.size() + " admin knowledge items to ChromaDB");
        } catch (Exception e) {
            System.err.println("Error syncing admin knowledge to ChromaDB: " + e.getMessage());
        }
    }

    public void clearAdminKnowledge() {
        try {
            restTemplate.delete(chromaDbServerUrl + "/admin/knowledge/clear");
            System.out.println("✓ Cleared existing admin knowledge in ChromaDB");
        } catch (Exception e) {
            System.err.println("Error clearing admin knowledge: " + e.getMessage());
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
}