package com.example.aipcbuilder.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "pc_components")
@Data
@NoArgsConstructor
@AllArgsConstructor
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

    public enum ComponentType {
        CPU, GPU, PSU, RAM, STORAGE, MOTHERBOARD, CASE
    }

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