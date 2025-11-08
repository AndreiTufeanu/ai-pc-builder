package com.example.aipcbuilder.dto;

public class LoginResponse {
    private String username;
    private String email;
    private String token;
    private String[] roles;

    // Constructors
    public LoginResponse() {}

    public LoginResponse(String username, String email, String token, String[] roles) {
        this.username = username;
        this.email = email;
        this.token = token;
        this.roles = roles;
    }

    // Getters and setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String[] getRoles() {
        return roles;
    }

    public void setRoles(String[] roles) {
        this.roles = roles;
    }
}