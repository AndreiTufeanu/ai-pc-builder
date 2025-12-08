package com.example.aipcbuilder.config;

import com.example.aipcbuilder.model.ChatMessage;
import com.example.aipcbuilder.model.PcComponent;
import com.example.aipcbuilder.repository.ChatMessageRepository;
import com.example.aipcbuilder.repository.PcComponentRepository;
import com.example.aipcbuilder.repository.UserRepository;
import com.example.aipcbuilder.service.chroma.ChromaDBService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChromaDBInitializer implements CommandLineRunner {

    private final PcComponentRepository componentRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ChromaDBService chromaDBService;

    @Override
    public void run(String... args) {
        // Messages cleanup
        log.info("Starting ChromaDB initialization...");
        chromaDBService.cleanupUserMessages();

        // Sync all components
        List<PcComponent> components = componentRepository.findAll();
        if (!components.isEmpty()) {
            log.info("Syncing {} components to ChromaDB...", components.size());
            chromaDBService.syncComponentsBatch(components);
        }

        // Clear and sync admin knowledge from all admin users
        chromaDBService.clearAdminKnowledge();
        List<Long> adminUserIds = userRepository.findAdminUserIds();
        if (!adminUserIds.isEmpty()) {
            log.info("Found {} admin users", adminUserIds.size());
            for (Long adminId : adminUserIds) {
                List<ChatMessage> adminMessages = chatMessageRepository.findByUserIdOrderByCreatedAtDesc(adminId);
                if (!adminMessages.isEmpty()) {
                    log.info("Syncing {} admin knowledge messages from user {}", adminMessages.size(), adminId);
                    chromaDBService.syncAdminKnowledge(adminMessages);
                }
            }
        }

        // Sync latest messages for all users (limited to 50 per user)
        List<Long> userIds = chatMessageRepository.findDistinctUserIds();
        for (Long userId : userIds) {
            List<ChatMessage> userMessages = chatMessageRepository.findByUserIdOrderByCreatedAtDesc(userId);
            if (!userMessages.isEmpty()) {
                int messagesToSync = Math.min(userMessages.size(), 50);
                log.info("Syncing {} messages for user {}", messagesToSync, userId);
                chromaDBService.syncLatestUserMessages(userMessages);
            }
        }

        String status = chromaDBService.getChromaDbStatus();
        log.info("ChromaDB initialization completed. Connection status: {}", status);
    }
}