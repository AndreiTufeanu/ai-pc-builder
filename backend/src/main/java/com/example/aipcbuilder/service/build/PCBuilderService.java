package com.example.aipcbuilder.service.build;

import com.example.aipcbuilder.service.chroma.ChromaDBService;
import com.example.aipcbuilder.service.helper.ContextBuilderHelper;
import com.example.aipcbuilder.service.helper.PromptBuilder;
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
    private final PromptBuilder promptBuilder;

    public PCBuilderService(ChatModel chatModel, ChromaDBService chromaDBService, ContextBuilderHelper contextBuilder, PromptBuilder promptBuilder) {
        this.chatClient = ChatClient.create(chatModel);
        this.chromaDBService = chromaDBService;
        this.contextBuilder = contextBuilder;
        this.promptBuilder = promptBuilder;
    }

    public String getChatResponse(String userMessage, Long userId) {
        System.out.println("=== User Chat Request ===");
        System.out.println("Message: " + userMessage);
        System.out.println("User ID: " + userId);

        List<Map<String, Object>> componentResults = chromaDBService.searchComponents(userMessage, 3, null);
        List<Map<String, Object>> knowledgeResults = chromaDBService.searchAdminKnowledge(userMessage, 2);
        List<Map<String, Object>> userContextResults = chromaDBService.searchUserMessagesByUser(userMessage, userId, 3);

        System.out.println("Found " + componentResults.size() + " relevant components");
        System.out.println("Found " + knowledgeResults.size() + " relevant knowledge items");
        System.out.println("Found " + userContextResults.size() + " relevant user context items");

        String context = contextBuilder.buildContext(componentResults, knowledgeResults, userContextResults);
        System.out.println("Context built: " + (context.length() > 0));

        String systemPrompt = promptBuilder.buildChatSystemPrompt(context);

        return chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content();
    }

    public String getAdminTrainingResponse(String userMessage) {
        Map<String, Object> metadata = Map.of(
                "timestamp", java.time.Instant.now().toString(),
                "source", "admin_training"
        );

        chromaDBService.addAdminKnowledge(userMessage, "TRAINING", metadata);

        List<Map<String, Object>> componentResults = chromaDBService.searchComponents(userMessage, 2, null);
        String context = componentResults.isEmpty() ? "" :
                "\nRelevant Components:\n" + contextBuilder.buildComponentContext(componentResults);

        String systemPrompt = promptBuilder.buildAdminTrainingSystemPrompt(context);

        return chatClient.prompt()
                .system(systemPrompt)
                .user("Training information: " + userMessage)
                .call()
                .content();
    }
}