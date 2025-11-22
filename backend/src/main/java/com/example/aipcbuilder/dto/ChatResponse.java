package com.example.aipcbuilder.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChatResponse {
    private String response;
    private Long messageId;

    // Constructors
    public ChatResponse() {}

    public ChatResponse(String response) {
        this.response = response;
    }

    public ChatResponse(String response, Long messageId) {
        this.response = response;
        this.messageId = messageId;
    }

}