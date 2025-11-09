package com.example.aipcbuilder.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "pc_components")
public class PcComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ComponentType type;

    private String description;
    private BigDecimal price;

    private String manufacturer;
    private String model;

    @Column(columnDefinition = "jsonb")
    private String specifications; // Store as JSON string

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Enums
    public enum ComponentType {
        CPU, GPU, PSU, RAM, STORAGE, MOTHERBOARD, CASE
    }

    // Constructors
    public PcComponent() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.specifications = "{}"; // Initialize as empty JSON
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ComponentType getType() {
        return type;
    }

    public void setType(ComponentType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setPrice(Double price) {
        this.price = BigDecimal.valueOf(price);
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSpecifications() {
        return specifications;
    }

    public void setSpecifications(String specifications) {
        this.specifications = specifications;
    }

    // Helper method to set specifications from a Map
    public void setSpecificationsMap(Map<String, Object> specifications) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.specifications = mapper.writeValueAsString(specifications);
        } catch (Exception e) {
            this.specifications = "{}";
        }
    }

    // Helper method to get specifications as a Map
    public Map<String, Object> getSpecificationsMap() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            if (this.specifications != null && !this.specifications.isEmpty()) {
                return mapper.readValue(this.specifications, new TypeReference<Map<String, Object>>() {});
            }
        } catch (Exception e) {
            // If there's an error parsing, return empty map
        }
        return new HashMap<>();
    }

    // Convenience methods for common specifications
    public String getSocket() {
        return (String) getSpecificationsMap().get("socket");
    }

    public String getMemoryType() {
        return (String) getSpecificationsMap().get("memoryType");
    }

    public String getFormFactor() {
        return (String) getSpecificationsMap().get("formFactor");
    }

    public Integer getTdp() {
        Object tdp = getSpecificationsMap().get("tdp");
        if (tdp instanceof Integer) {
            return (Integer) tdp;
        } else if (tdp instanceof Double) {
            return ((Double) tdp).intValue();
        }
        return null;
    }

    public Integer getCores() {
        Object cores = getSpecificationsMap().get("cores");
        if (cores instanceof Integer) {
            return (Integer) cores;
        } else if (cores instanceof Double) {
            return ((Double) cores).intValue();
        }
        return null;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}