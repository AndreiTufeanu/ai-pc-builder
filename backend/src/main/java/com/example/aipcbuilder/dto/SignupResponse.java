package com.example.aipcbuilder.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SignupResponse {
    private boolean success;
    private String message;

    // Constructors
    public SignupResponse() {}

    public SignupResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

}