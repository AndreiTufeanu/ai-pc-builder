package com.example.aipcbuilder.dto;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuildWithComponents {
    private Long id;
    private Long userId;
    private String name;
    private Double totalPrice;
    private LocalDateTime createdAt;

    private String cpu;
    private String gpu;
    private String psu;
    private String ram;
    private String storage;
    private String motherboard;
    private String pcCase;
}