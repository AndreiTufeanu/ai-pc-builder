package com.example.aipcbuilder.controller;

import com.example.aipcbuilder.dto.ChatRequest;
import com.example.aipcbuilder.dto.ChatResponse;
import com.example.aipcbuilder.model.ChatMessage;
import com.example.aipcbuilder.service.chat.ChatMessageService;
import com.example.aipcbuilder.service.chroma.ChromaDBService;
import com.example.aipcbuilder.service.build.PCBuilderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ChatController {

    private final PCBuilderService pcBuilderService;
    private final ChatMessageService chatMessageService;
    private final ChromaDBService chromaDBService;

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        System.out.println("Chat request from user ID: " + request.getUserId() + ", Message: " + request.getMessage());

        String aiResponse = pcBuilderService.getChatResponse(request.getMessage(), request.getUserId());
        ChatMessage savedMessage = chatMessageService.saveChatMessage(request.getUserId(), request.getMessage(), aiResponse);

        System.out.println("Chat message saved with ID: " + savedMessage.getId());

        List<ChatMessage> userMessages = chatMessageService.getUserChatHistory(request.getUserId());
        chromaDBService.syncLatestUserMessages(request.getUserId(), userMessages);

        return new ChatResponse(aiResponse, savedMessage.getId());
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<List<ChatMessage>> getChatHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(chatMessageService.getUserChatHistory(userId));
    }
}