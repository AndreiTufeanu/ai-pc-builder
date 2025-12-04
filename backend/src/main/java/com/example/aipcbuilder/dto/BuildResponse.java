package com.example.aipcbuilder.dto;

import com.example.aipcbuilder.model.Build;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuildResponse {
    private Build build;
    private String message;
    private boolean success;
}