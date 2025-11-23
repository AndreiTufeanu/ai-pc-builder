package com.example.aipcbuilder.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class BuildWithComponentsDTO {
    private Long id;
    private Long userId;
    private String name;
    private Double totalPrice;
    private LocalDateTime createdAt;

    // Component names instead of IDs
    private String cpu;
    private String gpu;
    private String psu;
    private String ram;
    private String storage;
    private String motherboard;
    private String pcCase;

    // Constructors
    public BuildWithComponentsDTO() {}

    public BuildWithComponentsDTO(Long id, Long userId, String name, Double totalPrice, LocalDateTime createdAt,
                                  String cpu, String gpu, String psu, String ram, String storage,
                                  String motherboard, String pcCase) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.totalPrice = totalPrice;
        this.createdAt = createdAt;
        this.cpu = cpu;
        this.gpu = gpu;
        this.psu = psu;
        this.ram = ram;
        this.storage = storage;
        this.motherboard = motherboard;
        this.pcCase = pcCase;
    }

}