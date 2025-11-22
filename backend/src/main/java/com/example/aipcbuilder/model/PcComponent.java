package com.example.aipcbuilder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
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

    @JsonIgnore
    public void setPriceFromDouble(Double price) {
        if (price != null) {
            this.price = BigDecimal.valueOf(price);
        } else {
            this.price = null;
        }
    }
}