package com.example.aipcbuilder.service.helper;

import com.example.aipcbuilder.model.PcComponent;
import com.example.aipcbuilder.repository.PcComponentRepository;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class ComponentSelector {

    private final PcComponentRepository componentRepository;

    public ComponentSelector(PcComponentRepository componentRepository) {
        this.componentRepository = componentRepository;
    }

    public PcComponent findComponentById(String componentId, String componentType) {
        if (componentId == null || componentId.trim().isEmpty()) {
            return null;
        }
        try {
            Long id = Long.parseLong(componentId.trim());
            Optional<PcComponent> component = componentRepository.findById(id);
            if (component.isPresent()) {
                return component.get();
            }
            System.err.println("Could not find component in database with ID: " + componentId + " for type: " + componentType);
            return null;
        } catch (NumberFormatException e) {
            System.err.println("Invalid component ID format: " + componentId + " for type: " + componentType);
            return null;
        } catch (Exception e) {
            System.err.println("Error finding component by ID: " + e.getMessage());
            return null;
        }
    }

    public String parseComponentIdFromResponse(String response, String componentType) {
        if (response == null) return null;

        String cleanResponse = response.replaceAll("[\"{}]", "").trim();
        String idOnly = cleanResponse.replaceAll("[^0-9]", "").trim();

        if (!idOnly.isEmpty()) {
            System.out.println("Parsed component ID: '" + idOnly + "' from response: '" + cleanResponse + "'");
            return idOnly;
        }

        System.err.println("Could not parse component ID from response: " + cleanResponse + " for type: " + componentType);
        return null;
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

        return query.toString().trim();
    }
}