package com.example.aipcbuilder.dto;

import com.example.aipcbuilder.model.Build;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class BuildResponse {
    private Build build;
    private String message;
    private boolean success;

    public BuildResponse() {}

    public BuildResponse(Build build, String message, boolean success) {
        this.build = build;
        this.message = message;
        this.success = success;
    }
}