package com.example.aipcbuilder.config;

import com.example.aipcbuilder.model.ChatMessage;
import com.example.aipcbuilder.model.PcComponent;
import com.example.aipcbuilder.repository.ChatMessageRepository;
import com.example.aipcbuilder.repository.PcComponentRepository;
import com.example.aipcbuilder.repository.UserRepository;
import com.example.aipcbuilder.service.chroma.ChromaDBService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChromaDBInitializer implements CommandLineRunner {

    private final PcComponentRepository componentRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ChromaDBService chromaDBService;


    public ChromaDBInitializer(PcComponentRepository componentRepository,
                               ChatMessageRepository chatMessageRepository,
                               ChromaDBService chromaDBService,
                               UserRepository userRepository) {
        this.componentRepository = componentRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.chromaDBService = chromaDBService;
        this.userRepository = userRepository;
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

        // Clear and sync admin knowledge from all admin users
        chromaDBService.clearAdminKnowledge();
        List<Long> adminUserIds = userRepository.findAdminUserIds();
        if (!adminUserIds.isEmpty()) {
            System.out.println("Found " + adminUserIds.size() + " admin users");
            for (Long adminId : adminUserIds) {
                List<ChatMessage> adminMessages = chatMessageRepository.findByUserIdOrderByCreatedAtDesc(adminId);
                if (!adminMessages.isEmpty()) {
                    System.out.println("Syncing " + adminMessages.size() + " admin knowledge messages from user " + adminId);
                    chromaDBService.syncAdminKnowledge(adminMessages);
                }
            }
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