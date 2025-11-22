package com.example.aipcbuilder.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LoginResponse {
    private Long userId;
    private String username;
    private String[] roles;
    private String message;

    // Constructors
    public LoginResponse() {}

    public LoginResponse(Long userId, String username, String[] roles, String message) {
        this.userId = userId;
        this.username = username;
        this.roles = roles;
        this.message = message;
    }

}