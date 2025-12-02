package com.example.aipcbuilder.service;

import com.example.aipcbuilder.model.ChatMessage;
import com.example.aipcbuilder.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;

    public ChatMessageService(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    public ChatMessage saveChatMessage(Long userId, String userMessage, String aiResponse) {
        ChatMessage chatMessage = new ChatMessage(userId, userMessage, aiResponse);
        return chatMessageRepository.save(chatMessage);
    }
    
    public List<ChatMessage> getUserChatHistory(Long userId) {
        return chatMessageRepository.findByUserIdOrderByCreatedAt(userId);
    }
}