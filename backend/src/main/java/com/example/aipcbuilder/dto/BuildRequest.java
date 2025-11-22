package com.example.aipcbuilder.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.Map;

@Setter
@Getter
public class BuildRequest {
    private Long userId;
    private String name;
    private String description;
    private BigDecimal budget;
    private Map<String, Map<String, Object>> requirements;

    public BuildRequest() {}
}