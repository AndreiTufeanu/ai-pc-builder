package com.example.aipcbuilder.controller;

import com.example.aipcbuilder.dto.ChatRequest;
import com.example.aipcbuilder.dto.ChatResponse;
import com.example.aipcbuilder.service.PCBuilderService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final PCBuilderService pcBuilderService;

    public ChatController(PCBuilderService pcBuilderService) {
        this.pcBuilderService = pcBuilderService;
    }

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String response = pcBuilderService.getChatResponse(request.getMessage());
        return new ChatResponse(response);
    }
}