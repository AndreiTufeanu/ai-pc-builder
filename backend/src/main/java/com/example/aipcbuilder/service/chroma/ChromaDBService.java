package com.example.aipcbuilder.service.chroma;

import com.example.aipcbuilder.model.ChatMessage;
import com.example.aipcbuilder.model.PcComponent;
import com.example.aipcbuilder.service.chroma.client.ChromaDBClient;
import com.example.aipcbuilder.service.chroma.collections.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChromaDBService {

    private final ChromaDBClient client;
    private final ComponentCollectionService componentCollection;
    private final KnowledgeCollectionService knowledgeCollection;
    private final UserMessagesCollectionService userMessagesCollection;

    // Component operations
    public void syncComponent(PcComponent component) {
        componentCollection.upsert(component);
    }

    public void syncComponentsBatch(List<PcComponent> components) {
        componentCollection.upsert(components);
    }

    public void deleteComponent(Long componentId) {
        componentCollection.delete(componentId);
    }

    public List<Map<String, Object>> searchComponents(String query, int nResults, String componentType) {
        return componentCollection.search(query, nResults, componentType);
    }

    // Admin knowledge operations
    public void addAdminKnowledge(String content, String knowledgeType, Map<String, Object> metadata) {
        knowledgeCollection.addKnowledge(content, knowledgeType, metadata);
    }

    public void clearAdminKnowledge() {
        knowledgeCollection.clear();
    }

    public List<Map<String, Object>> searchAdminKnowledge(String query, int nResults) {
        return knowledgeCollection.search(query, nResults);
    }

    public void syncAdminKnowledge(List<ChatMessage> adminMessages) {
        knowledgeCollection.syncMessages(adminMessages);
    }

    public List<Map<String, Object>> searchUserMessagesByUser(String query, Long userId, int nResults) {
        return userMessagesCollection.searchByUser(query, userId, nResults);
    }

    public void syncLatestUserMessages(List<ChatMessage> messages) {
        userMessagesCollection.syncLatest(messages);
    }

    // Health and maintenance operations
    public void performStartupCleanup() {
        userMessagesCollection.cleanup();
    }

    public String getChromaDbStatus() {
        return client.getStatus();
    }
}