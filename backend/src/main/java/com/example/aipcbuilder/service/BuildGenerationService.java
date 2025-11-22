package com.example.aipcbuilder.service;

import com.example.aipcbuilder.model.Build;
import com.example.aipcbuilder.model.PcComponent;
import com.example.aipcbuilder.repository.PcComponentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class BuildGenerationService {

    private final PcComponentRepository componentRepository;

    public BuildGenerationService(PcComponentRepository componentRepository) {
        this.componentRepository = componentRepository;
    }

    /**
     * Generate a complete PC build based on requirements
     * Requirements are used for generation but not stored in the Build entity
     */
    public Build generateBuild(Build build, Map<String, Map<String, Object>> requirements) {
        BigDecimal totalPrice = BigDecimal.ZERO;

        // Generate components for each type based on requirements
        for (Map.Entry<String, Map<String, Object>> entry : requirements.entrySet()) {
            String componentType = entry.getKey();
            Map<String, Object> specs = entry.getValue();

            PcComponent selectedComponent = selectComponentForType(componentType, specs);

            if (selectedComponent != null) {
                // Set the component ID in the build
                setComponentId(build, componentType, selectedComponent.getId());

                // Add to total price
                if (selectedComponent.getPrice() != null) {
                    totalPrice = totalPrice.add(selectedComponent.getPrice());
                }
            }
        }

        build.setTotalPrice(totalPrice);
        return build;
    }

    private PcComponent selectComponentForType(String componentType, Map<String, Object> requirements) {
        // For now, just select the first available component of this type
        try {
            PcComponent.ComponentType type = PcComponent.ComponentType.valueOf(componentType);
            List<PcComponent> components = componentRepository.findByType(type);

            if (!components.isEmpty()) {
                return components.get(0); // Simple selection for now
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid component type: " + componentType);
        }

        return null;
    }

    private void setComponentId(Build build, String componentType, Long componentId) {
        switch (componentType) {
            case "CPU":
                build.setCpuId(componentId);
                break;
            case "GPU":
                build.setGpuId(componentId);
                break;
            case "PSU":
                build.setPsuId(componentId);
                break;
            case "RAM":
                build.setRamId(componentId);
                break;
            case "STORAGE":
                build.setStorageId(componentId);
                break;
            case "MOTHERBOARD":
                build.setMotherboardId(componentId);
                break;
            case "CASE":
                build.setCaseId(componentId);
                break;
        }
    }
}