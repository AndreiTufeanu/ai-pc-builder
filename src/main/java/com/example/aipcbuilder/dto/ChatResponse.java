package com.example.aipcbuilder.dto;

public class ChatResponse {
    private String response;

    // Constructors
    public ChatResponse() {}

    public ChatResponse(String response) {
        this.response = response;
    }

    // Getters and setters
    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}