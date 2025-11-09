package com.example.aipcbuilder.dto;

public class LoginResponse {
    private String username;
    private String[] roles;
    private String message;

    // Constructors
    public LoginResponse() {}

    public LoginResponse(String username, String[] roles, String message) {
        this.username = username;
        this.roles = roles;
        this.message = message;
    }

    // Getters and setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String[] getRoles() {
        return roles;
    }

    public void setRoles(String[] roles) {
        this.roles = roles;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}