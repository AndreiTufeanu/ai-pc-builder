package com.example.aipcbuilder.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.Map;

@Setter
@Getter
public class AIBuildResponse {
    private Map<String, Long> componentIds; // component type -> component ID
    private String reasoning;
    private boolean success;
    private String message;

    public AIBuildResponse() {}

    public AIBuildResponse(Map<String, Long> componentIds, String reasoning, boolean success, String message) {
        this.componentIds = componentIds;
        this.reasoning = reasoning;
        this.success = success;
        this.message = message;
    }
}