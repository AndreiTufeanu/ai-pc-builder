package com.example.aipcbuilder.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChatRequest {
    private String message;
    private Long userId;

    // Constructors
    public ChatRequest() {}

    public ChatRequest(String message) {
        this.message = message;
    }

    public ChatRequest(String message, Long userId) {
        this.message = message;
        this.userId = userId;
    }

}