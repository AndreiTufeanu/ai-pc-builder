package com.example.aipcbuilder.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SignupRequest {
    private String username;
    private String password;
    private String confirmPassword;

    // Constructors
    public SignupRequest() {}

    public SignupRequest(String username, String password, String confirmPassword) {
        this.username = username;
        this.password = password;
        this.confirmPassword = confirmPassword;
    }

}