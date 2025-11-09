package com.example.aipcbuilder.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

@Service
public class PCBuilderService {

    private final ChatClient chatClient;

    public PCBuilderService(ChatModel chatModel) {
        this.chatClient = ChatClient.create(chatModel);
    }

    public String getChatResponse(String userMessage) {
        String systemPrompt = """
            You are an expert PC building assistant. Help users choose compatible PC components.
            When suggesting parts, be specific about compatibility requirements.
            Keep responses concise and helpful.
            """;

        return chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content();
    }
}