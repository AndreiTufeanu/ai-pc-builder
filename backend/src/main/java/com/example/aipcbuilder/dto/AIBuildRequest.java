package com.example.aipcbuilder.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIBuildRequest {

    @DecimalMin(value = "300.00", message = "Minimum budget is $300")
    private Double budget;

    private Map<String, Map<String, Object>> requirements;
}