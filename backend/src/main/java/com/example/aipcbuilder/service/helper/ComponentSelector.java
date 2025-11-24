package com.example.aipcbuilder.service.helper;

import com.example.aipcbuilder.model.PcComponent;
import com.example.aipcbuilder.repository.PcComponentRepository;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ComponentSelector {

    private final PcComponentRepository componentRepository;

    public ComponentSelector(PcComponentRepository componentRepository) {
        this.componentRepository = componentRepository;
    }

    public PcComponent findComponentByName(String componentName, String componentType) {
        if (componentName == null || componentName.trim().isEmpty()) {
            return null;
        }
        try {
            String cleanName = componentName.trim();
            PcComponent component = componentRepository.findByName(cleanName);
            if (component != null) {
                return component;
            }
            System.err.println("Could not find component in database: " + cleanName + " for type: " + componentType);
            return null;
        } catch (Exception e) {
            System.err.println("Error finding component by name: " + e.getMessage());
            return null;
        }
    }

    public String buildComponentSpecificQuery(String componentType,
                                              Map<String, Map<String, Object>> requirements,
                                              Map<String, PcComponent> alreadySelected,
                                              double remainingBudget,
                                              int iteration) {
        StringBuilder query = new StringBuilder();
        query.append(componentType).append(" ");

        if (iteration > 1) {
            query.append("budget cost-effective affordable ");
        }
        query.append("Budget $").append(remainingBudget).append(" ");

        if (requirements != null && requirements.containsKey(componentType.toUpperCase())) {
            Map<String, Object> componentReqs = requirements.get(componentType.toUpperCase());
            Map<String, Object> specs = (Map<String, Object>) componentReqs.get("specifications");
            if (specs != null) {
                for (Object value : specs.values()) {
                    query.append(value).append(" ");
                }
            }
        }

        if (alreadySelected.containsKey("cpu") && componentType.equals("MOTHERBOARD")) {
            PcComponent cpu = alreadySelected.get("cpu");
            if (cpu != null && cpu.getSpecifications() != null) {
                Object socket = cpu.getSpecifications().get("socket");
                if (socket != null) {
                    query.append(socket).append(" ");
                }
            }
        }
        if (alreadySelected.containsKey("motherboard") && componentType.equals("RAM")) {
            PcComponent motherboard = alreadySelected.get("motherboard");
            if (motherboard != null && motherboard.getSpecifications() != null) {
                Object memoryType = motherboard.getSpecifications().get("memoryType");
                if (memoryType != null) {
                    query.append(memoryType).append(" ");
                } else {
                    query.append("DDR5 ");
                }
            }
        }

        return query.toString().trim();
    }

    public String parseComponentNameFromResponse(String response, String componentType) {
        String cleanResponse = response.replaceAll("[\"{}]", "").trim();
        if (cleanResponse.contains(":")) {
            String[] parts = cleanResponse.split(":");
            if (parts.length == 2) {
                return parts[1].trim();
            }
        }
        return cleanResponse;
    }
}