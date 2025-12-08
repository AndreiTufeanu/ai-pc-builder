package com.example.aipcbuilder.service.chroma.collections;

import com.example.aipcbuilder.model.ChatMessage;
import com.example.aipcbuilder.service.chroma.client.ChromaDBClient;
import com.example.aipcbuilder.service.chroma.helper.ChromaDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.core.ParameterizedTypeReference;

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

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> searchByUser(String query, Long userId, int limit) {
        Map<String, Object> searchRequest = new HashMap<>();
        searchRequest.put("query", query);
        searchRequest.put("collection", COLLECTION_NAME);
        searchRequest.put("n_results", limit);
        searchRequest.put("where", Map.of("user_id", userId.toString()));

        try {
            Map<String, Object> response = client.post(
                    SEARCH_ENDPOINT,
                    searchRequest,
                    new ParameterizedTypeReference<>() {}
            );
            return (List<Map<String, Object>>) response.getOrDefault("results", List.of());
        } catch (Exception e) {
            log.error("Failed to search user messages for user {}: {}", userId, e.getMessage());
            return List.of();
        }
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