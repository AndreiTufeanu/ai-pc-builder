package com.example.aipcbuilder.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.Map;

@Setter
@Getter
public class AIBuildRequest {
    private Double budget;
    private Map<String, Map<String, Object>> requirements;

    public AIBuildRequest() {}

    public AIBuildRequest(Double budget, Map<String, Map<String, Object>> requirements) {
        this.budget = budget;
        this.requirements = requirements;
    }
}