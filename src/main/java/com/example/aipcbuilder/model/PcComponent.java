package com.example.aipcbuilder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
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

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> specifications = new HashMap<>();

    // Enums
    public enum ComponentType {
        CPU, GPU, PSU, RAM, STORAGE, MOTHERBOARD, CASE
    }

    // Constructors
    public PcComponent() {}

    public PcComponent(String name, ComponentType type, String description, BigDecimal price,
                       String manufacturer, String model, Map<String, Object> specifications) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.price = price;
        this.manufacturer = manufacturer;
        this.model = model;
        this.specifications = specifications;
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

    @JsonIgnore
    public void setPriceFromDouble(Double price) {
        if (price != null) {
            this.price = BigDecimal.valueOf(price);
        } else {
            this.price = null;
        }
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

    public Map<String, Object> getSpecifications() {
        return specifications;
    }

    public void setSpecifications(Map<String, Object> specifications) {
        this.specifications = specifications;
    }

    // Convenience methods for common specifications
    @JsonIgnore
    public String getSocket() {
        return (String) specifications.get("socket");
    }

    @JsonIgnore
    public String getMemoryType() {
        return (String) specifications.get("memoryType");
    }

    @JsonIgnore
    public String getFormFactor() {
        return (String) specifications.get("formFactor");
    }

    @JsonIgnore
    public Integer getTdp() {
        Object tdp = specifications.get("tdp");
        if (tdp instanceof Integer) {
            return (Integer) tdp;
        } else if (tdp instanceof Double) {
            return ((Double) tdp).intValue();
        }
        return null;
    }

    @JsonIgnore
    public Integer getCores() {
        Object cores = specifications.get("cores");
        if (cores instanceof Integer) {
            return (Integer) cores;
        } else if (cores instanceof Double) {
            return ((Double) cores).intValue();
        }
        return null;
    }
}