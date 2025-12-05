package com.example.aipcbuilder.service.chroma.collections;

import com.example.aipcbuilder.model.ChatMessage;
import com.example.aipcbuilder.service.chroma.client.ChromaDBClient;
import com.example.aipcbuilder.service.chroma.helper.ChromaDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserMessagesCollectionService {

    private static final String COLLECTION_NAME = "user_messages";
    private static final String UPSERT_ENDPOINT = "/user_messages/upsert";
    private static final String CLEANUP_ENDPOINT = "/user_messages/cleanup/startup";
    private static final String SEARCH_ENDPOINT = "/search";

    private final ChromaDBClient client;
    private final ChromaDataService dataHelper;

    public void upsert(List<ChatMessage> messages) {
        List<Map<String, Object>> messageData = messages.stream()
                .map(dataHelper::createUserMessageData)
                .collect(Collectors.toList());

        try {
            client.post(UPSERT_ENDPOINT, messageData, String.class);
            log.debug("Upserted {} user messages", messages.size());
        } catch (Exception e) {
            log.error("Failed to upsert user messages: {}", e.getMessage());
        }
    }

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
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return (List<Map<String, Object>>) response.getOrDefault("results", List.of());
        } catch (Exception e) {
            log.error("Failed to search user messages: {}", e.getMessage());
            return List.of();
        }
    }

    public List<Map<String, Object>> searchByUser(String query, Long userId, int limit) {
        List<Map<String, Object>> results = search(query, limit * 2);

        return results.stream()
                .filter(result -> {
                    Map<String, Object> metadata = (Map<String, Object>) result.get("metadata");
                    String resultUserId = (String) metadata.get("user_id");
                    return userId.toString().equals(resultUserId);
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    public void cleanup() {
        try {
            client.post(CLEANUP_ENDPOINT, null, String.class);
            log.info("Performed user messages cleanup");
        } catch (Exception e) {
            log.error("Failed to cleanup user messages: {}", e.getMessage());
        }
    }

    public void syncLatest(List<ChatMessage> messages) {
        List<ChatMessage> latestMessages = messages.stream()
                .filter(msg -> msg.getCreatedAt() != null)
                .sorted((m1, m2) -> m2.getCreatedAt().compareTo(m1.getCreatedAt()))
                .limit(50)
                .collect(Collectors.toList());

        upsert(latestMessages);
    }
}