package com.example.aipcbuilder.dto;

public class ChatResponse {
    private String response;
    private Long messageId; // Added to track the message in database

    // Constructors
    public ChatResponse() {}

    public ChatResponse(String response) {
        this.response = response;
    }

    public ChatResponse(String response, Long messageId) {
        this.response = response;
        this.messageId = messageId;
    }

    // Getters and setters
    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }
}