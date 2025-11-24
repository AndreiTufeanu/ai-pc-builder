package com.example.aipcbuilder.service;

import com.example.aipcbuilder.service.helper.ContextBuilderHelper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PCBuilderService {

    private final ChatClient chatClient;
    private final ChromaDBService chromaDBService;
    private final ContextBuilderHelper contextBuilder;

    public PCBuilderService(ChatModel chatModel, ChromaDBService chromaDBService, ContextBuilderHelper contextBuilder) {
        this.chatClient = ChatClient.create(chatModel);
        this.chromaDBService = chromaDBService;
        this.contextBuilder = contextBuilder;
    }

    public String getChatResponse(String userMessage, Long userId) {
        System.out.println("=== User Chat Request ===");
        System.out.println("Message: " + userMessage);
        System.out.println("User ID: " + userId);

        // Search for relevant context
        List<Map<String, Object>> componentResults = chromaDBService.searchComponents(userMessage, 3);
        List<Map<String, Object>> knowledgeResults = chromaDBService.searchAdminKnowledge(userMessage, 2);
        List<Map<String, Object>> userContextResults = chromaDBService.searchUserMessagesByUser(userMessage, userId, 3);

        System.out.println("Found " + componentResults.size() + " relevant components");
        System.out.println("Found " + knowledgeResults.size() + " relevant knowledge items");
        System.out.println("Found " + userContextResults.size() + " relevant user context items");

        String context = contextBuilder.buildContext(componentResults, knowledgeResults, userContextResults);
        System.out.println("Context built: " + (context.length() > 0));

        String systemPrompt = """
        You are an expert PC building assistant. Help users choose compatible PC components.
        When suggesting parts, be specific about compatibility requirements.
        Keep responses concise and helpful.
        
        Available Components, Knowledge, and Conversation Context:
        %s
        
        Instructions:
        - Use the component information above when relevant
        - Consider compatibility between components
        - Suggest specific components when appropriate
        - Reference previous conversation context when relevant
        - If you don't have information, say so
        """.formatted(context);

        return chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content();
    }

    public String getAdminTrainingResponse(String userMessage) {
        // Add admin message to knowledge base (no user ID)
        Map<String, Object> metadata = Map.of(
                "timestamp", java.time.Instant.now().toString(),
                "source", "admin_training"
        );

        chromaDBService.addAdminKnowledge(userMessage, "TRAINING", metadata);

        // Search for relevant components to provide context
        List<Map<String, Object>> componentResults = chromaDBService.searchComponents(userMessage, 2);
        String context = componentResults.isEmpty() ? "" :
                "\nRelevant Components:\n" + contextBuilder.buildComponentContext(componentResults);

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
}