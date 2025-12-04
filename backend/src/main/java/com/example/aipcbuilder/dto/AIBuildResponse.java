package com.example.aipcbuilder.dto;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIBuildResponse {
    private Map<String, Long> componentIds;
    private String reasoning;
    private boolean success;
    private String message;
}