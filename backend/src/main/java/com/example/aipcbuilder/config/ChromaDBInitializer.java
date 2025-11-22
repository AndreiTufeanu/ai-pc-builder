package com.example.aipcbuilder.config;

import com.example.aipcbuilder.model.ChatMessage;
import com.example.aipcbuilder.model.PcComponent;
import com.example.aipcbuilder.repository.ChatMessageRepository;
import com.example.aipcbuilder.repository.PcComponentRepository;
import com.example.aipcbuilder.service.ChromaDBService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChromaDBInitializer implements CommandLineRunner {

    private final PcComponentRepository componentRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChromaDBService chromaDBService;

    public ChromaDBInitializer(PcComponentRepository componentRepository,
                               ChatMessageRepository chatMessageRepository,
                               ChromaDBService chromaDBService) {
        this.componentRepository = componentRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.chromaDBService = chromaDBService;
    }

    @Override
    public void run(String... args) throws Exception {
        // Perform startup cleanup first
        System.out.println("Performing ChromaDB startup cleanup...");
        chromaDBService.performStartupCleanup();

        // Sync all components
        List<PcComponent> components = componentRepository.findAll();
        if (!components.isEmpty()) {
            System.out.println("Syncing " + components.size() + " components to ChromaDB...");
            chromaDBService.syncComponentsBatch(components); // Updated method name
        }

        // Sync latest messages for all users (limited to 50 per user)
        List<Long> userIds = chatMessageRepository.findDistinctUserIds();
        for (Long userId : userIds) {
            List<ChatMessage> userMessages = chatMessageRepository.findByUserIdOrderByCreatedAtDesc(userId);
            if (!userMessages.isEmpty()) {
                System.out.println("Syncing " + Math.min(userMessages.size(), 50) + " messages for user " + userId);
                chromaDBService.syncLatestUserMessages(userId, userMessages);
            }
        }

        String status = chromaDBService.getChromaDbStatus();
        System.out.println("ChromaDB connection status: " + status);
    }
}