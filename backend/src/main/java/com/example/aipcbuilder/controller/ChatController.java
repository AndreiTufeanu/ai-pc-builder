package com.example.aipcbuilder.controller;

import com.example.aipcbuilder.dto.ChatRequest;
import com.example.aipcbuilder.dto.ChatResponse;
import com.example.aipcbuilder.model.ChatMessage;
import com.example.aipcbuilder.service.ChatMessageService;
import com.example.aipcbuilder.service.PCBuilderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final PCBuilderService pcBuilderService;
    private final ChatMessageService chatMessageService;

    public ChatController(PCBuilderService pcBuilderService, ChatMessageService chatMessageService) {
        this.pcBuilderService = pcBuilderService;
        this.chatMessageService = chatMessageService;
    }

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        System.out.println("Chat request from user ID: " + request.getUserId());
        System.out.println("Message: " + request.getMessage());

        // Get AI response first
        String aiResponse = pcBuilderService.getChatResponse(request.getMessage());

        // Save both user message and AI response to database
        ChatMessage savedMessage = chatMessageService.saveChatMessage(
                request.getUserId(),
                request.getMessage(),
                aiResponse
        );

        System.out.println("Chat message saved with ID: " + savedMessage.getId());

        return new ChatResponse(aiResponse, savedMessage.getId());
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<List<ChatMessage>> getChatHistory(@PathVariable Long userId) {
        List<ChatMessage> messages = chatMessageService.getUserChatHistory(userId);
        return ResponseEntity.ok(messages);
    }
}