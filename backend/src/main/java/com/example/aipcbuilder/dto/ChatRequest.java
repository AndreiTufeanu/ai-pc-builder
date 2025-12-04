package com.example.aipcbuilder.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    @NotNull(message = "User ID is required")
    @Min(value = 1, message = "User ID must be positive")
    private Long userId;

    @NotBlank(message = "Message is required")
    @Size(min = 1, max = 1000, message = "Message must be between 1 and 1000 characters")
    private String message;
}