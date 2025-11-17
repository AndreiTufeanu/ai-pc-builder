package com.example.aipcbuilder.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PCBuilderService {

    private final ChatClient chatClient;
    private final ChromaDBService chromaDBService;

    public PCBuilderService(ChatModel chatModel, ChromaDBService chromaDBService) {
        this.chatClient = ChatClient.create(chatModel);
        this.chromaDBService = chromaDBService;
    }

    public String getChatResponse(String userMessage) {
        // Search for relevant components and knowledge
        List<Map<String, Object>> componentResults = chromaDBService.searchComponents(userMessage, 3);
        List<Map<String, Object>> knowledgeResults = chromaDBService.searchAdminKnowledge(userMessage, 2);

        String context = buildContext(componentResults, knowledgeResults);

        String systemPrompt = """
            You are an expert PC building assistant. Help users choose compatible PC components.
            When suggesting parts, be specific about compatibility requirements.
            Keep responses concise and helpful.
            
            Available Components and Knowledge:
            %s
            
            Instructions:
            - Use the component information above when relevant
            - Consider compatibility between components
            - Suggest specific components when appropriate
            - If you don't have information, say so
            """.formatted(context);

        return chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content();
    }

    public String getAdminTrainingResponse(String userMessage, Long userId) {
        // Add admin message to knowledge base
        Map<String, Object> metadata = Map.of(
                "user_id", userId.toString(),
                "timestamp", java.time.Instant.now().toString(),
                "source", "admin_training"
        );

        chromaDBService.addAdminKnowledge(userMessage, "TRAINING", metadata);

        // Also search for relevant context
        List<Map<String, Object>> componentResults = chromaDBService.searchComponents(userMessage, 2);

        String context = componentResults.isEmpty() ? "" :
                "\nRelevant Components:\n" + buildComponentContext(componentResults);

        String systemPrompt = """
            You are receiving training information about PC components and building.
            Acknowledge the information and explain how it will help improve recommendations.
            
            %s
            
            Respond professionally and thank the admin for the training data.
            """.formatted(context);

        return chatClient.prompt()
                .system(systemPrompt)
                .user("Training information: " + userMessage)
                .call()
                .content();
    }

    private String buildContext(List<Map<String, Object>> componentResults,
                                List<Map<String, Object>> knowledgeResults) {
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

        return context.toString();
    }

    private String buildComponentContext(List<Map<String, Object>> componentResults) {
        return componentResults.stream()
                .map(result -> {
                    Map<String, Object> metadata = (Map<String, Object>) result.get("metadata");
                    String doc = (String) result.get("document");
                    // Extract the first few lines for brevity
                    return doc.lines()
                            .limit(4)
                            .collect(Collectors.joining("\n"));
                })
                .collect(Collectors.joining("\n---\n"));
    }
}