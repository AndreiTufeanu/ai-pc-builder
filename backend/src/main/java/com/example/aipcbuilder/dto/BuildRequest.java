package com.example.aipcbuilder.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuildRequest {

    @NotNull(message = "User ID is required")
    @Min(value = 1, message = "User ID must be positive")
    private Long userId;

    @NotBlank(message = "Build name is required")
    @Size(min = 3, max = 50, message = "Build name must be between 3 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @DecimalMin(value = "300.00", message = "Minimum budget is $300")
    private BigDecimal budget;

    private Map<String, Map<String, Object>> requirements;
}