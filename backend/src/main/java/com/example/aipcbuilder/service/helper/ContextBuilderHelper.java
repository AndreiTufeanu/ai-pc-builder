package com.example.aipcbuilder.service.helper;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ContextBuilderHelper {

    public String buildContext(List<Map<String, Object>> componentResults,
                               List<Map<String, Object>> knowledgeResults,
                               List<Map<String, Object>> userContextResults) {
        StringBuilder context = new StringBuilder();

        if (!componentResults.isEmpty()) {
            context.append("=== AVAILABLE COMPONENTS ===\n");
            context.append(buildComponentContext(componentResults));
        }

        if (!knowledgeResults.isEmpty()) {
            context.append("\n=== EXPERT KNOWLEDGE ===\n");
            for (Map<String, Object> result : knowledgeResults) {
                String doc = (String) result.get("document");
                context.append("- ").append(doc).append("\n");
            }
        }

        if (!userContextResults.isEmpty()) {
            context.append("\n=== PREVIOUS CONVERSATION ===\n");
            for (Map<String, Object> result : userContextResults) {
                String doc = (String) result.get("document");
                String preview = doc.lines().limit(3).collect(Collectors.joining("\n"));
                context.append("- ").append(preview).append("\n");
            }
        }

        return context.toString();
    }

    public String buildComponentContext(List<Map<String, Object>> componentResults) {
        return componentResults.stream()
                .map(result -> (String) result.get("document"))
                .collect(Collectors.joining("\n---\n"));
    }

    public String buildKnowledgeContext(List<Map<String, Object>> knowledgeResults) {
        if (knowledgeResults.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder("\n=== EXPERT KNOWLEDGE ===\n");
        for (Map<String, Object> result : knowledgeResults) {
            String doc = (String) result.get("document");
            Map<String, Object> metadata = (Map<String, Object>) result.get("metadata");
            String knowledgeType = (String) metadata.get("knowledge_type");
            context.append("[").append(knowledgeType).append("] ").append(doc).append("\n");
        }
        return context.toString();
    }
}