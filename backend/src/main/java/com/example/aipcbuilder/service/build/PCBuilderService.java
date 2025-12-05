package com.example.aipcbuilder.service.build;

import com.example.aipcbuilder.service.chroma.ChromaDBService;
import com.example.aipcbuilder.service.build.helper.ContextBuilderService;
import com.example.aipcbuilder.service.build.helper.PromptBuilderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class PCBuilderService {

    private final ChatClient chatClient;
    private final ChromaDBService chromaDBService;
    private final ContextBuilderService contextBuilder;
    private final PromptBuilderService promptBuilderService;

    public PCBuilderService(ChatModel chatModel, ChromaDBService chromaDBService, ContextBuilderService contextBuilder, PromptBuilderService promptBuilderService) {
        this.chatClient = ChatClient.create(chatModel);
        this.chromaDBService = chromaDBService;
        this.contextBuilder = contextBuilder;
        this.promptBuilderService = promptBuilderService;
    }

    public String getChatResponse(String userMessage, Long userId) {
        log.info("=== User Chat Request ===");
        log.info("Message: {}", userMessage);
        log.info("User ID: {}", userId);

        List<Map<String, Object>> componentResults = chromaDBService.searchComponents(userMessage, 3, null);
        List<Map<String, Object>> knowledgeResults = chromaDBService.searchAdminKnowledge(userMessage, 2);
        List<Map<String, Object>> userContextResults = chromaDBService.searchUserMessagesByUser(userMessage, userId, 3);

        log.debug("Found {} relevant components", componentResults.size());
        log.debug("Found {} relevant knowledge items", knowledgeResults.size());
        log.debug("Found {} relevant user context items", userContextResults.size());

        String context = contextBuilder.buildContext(componentResults, knowledgeResults, userContextResults);
        log.debug("Context built: {}", !context.isEmpty());

        String systemPrompt = promptBuilderService.buildChatSystemPrompt(context);

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

        String systemPrompt = promptBuilderService.buildAdminTrainingSystemPrompt(context);

        return chatClient.prompt()
                .system(systemPrompt)
                .user("Training information: " + userMessage)
                .call()
                .content();
    }
}