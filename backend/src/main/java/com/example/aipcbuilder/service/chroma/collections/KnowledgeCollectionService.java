package com.example.aipcbuilder.service.chroma.collections;

import com.example.aipcbuilder.model.ChatMessage;
import com.example.aipcbuilder.service.chroma.client.ChromaDBClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.springframework.core.ParameterizedTypeReference;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeCollectionService {

    private static final String COLLECTION_NAME = "admin_knowledge";
    private static final String KNOWLEDGE_ENDPOINT = "/admin/knowledge";
    private static final String CLEAR_ENDPOINT = "/admin/knowledge/clear";
    private static final String SEARCH_ENDPOINT = "/search";

    private final ChromaDBClient client;

    public void addKnowledge(String content, String knowledgeType, Map<String, Object> metadata) {
        Map<String, Object> knowledgeData = Map.of(
                "content", content,
                "knowledge_type", knowledgeType,
                "metadata", metadata != null ? metadata : Map.of()
        );

        try {
            client.post(KNOWLEDGE_ENDPOINT, knowledgeData, String.class);
            log.debug("Added admin knowledge to ChromaDB");
        } catch (Exception e) {
            log.error("Failed to add admin knowledge: {}", e.getMessage());
        }
    }

    public void clear() {
        try {
            client.delete(CLEAR_ENDPOINT);
            log.info("Cleared admin knowledge collection");
        } catch (Exception e) {
            log.error("Failed to clear admin knowledge: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> search(String query, int limit) {
        Map<String, Object> searchRequest = Map.of(
                "query", query,
                "collection", COLLECTION_NAME,
                "n_results", limit
        );

        try {
            Map<String, Object> response = client.post(
                    SEARCH_ENDPOINT,
                    searchRequest,
                    new ParameterizedTypeReference<>() {
                    }
            );
            return (List<Map<String, Object>>) response.getOrDefault("results", List.of());
        } catch (Exception e) {
            log.error("Failed to search collection {}: {}", COLLECTION_NAME, e.getMessage());
            return List.of();
        }
    }

    public void syncMessages(List<ChatMessage> messages) {
        messages.forEach(message -> {
            Map<String, Object> metadata = Map.of(
                    "timestamp", message.getCreatedAt().toString(),
                    "source", "admin_training"
            );
            addKnowledge(message.getUserMessage(), "TRAINING", metadata);
        });
        log.info("Synced {} admin knowledge messages", messages.size());
    }
}