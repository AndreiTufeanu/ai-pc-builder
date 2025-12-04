package com.example.aipcbuilder.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pc_builds")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Build {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "budget", precision = 10, scale = 2)
    private BigDecimal budget;

    @Column(name = "cpu_id")
    private Long cpuId;

    @Column(name = "gpu_id")
    private Long gpuId;

    @Column(name = "psu_id")
    private Long psuId;

    @Column(name = "ram_id")
    private Long ramId;

    @Column(name = "storage_id")
    private Long storageId;

    @Column(name = "motherboard_id")
    private Long motherboardId;

    @Column(name = "case_id")
    private Long caseId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Build(Long userId, String name, String description, BigDecimal budget) {
        this();
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.budget = budget;
    }
}