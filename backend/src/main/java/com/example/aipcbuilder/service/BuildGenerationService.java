// BuildGenerationService.java
package com.example.aipcbuilder.service;

import com.example.aipcbuilder.dto.AIBuildRequest;
import com.example.aipcbuilder.dto.AIBuildResponse;
import com.example.aipcbuilder.model.Build;
import com.example.aipcbuilder.model.PcComponent;
import com.example.aipcbuilder.repository.PcComponentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class BuildGenerationService {

    private final PcComponentRepository componentRepository;
    private final AIBuildService aiBuildService;

    public BuildGenerationService(PcComponentRepository componentRepository, AIBuildService aiBuildService) {
        this.componentRepository = componentRepository;
        this.aiBuildService = aiBuildService;
    }

    /**
     * Generate a complete PC build using AI based on requirements
     */
    public Build generateBuild(Build build, Map<String, Map<String, Object>> requirements) {
        System.out.println("=== AI Build Generation ===");

        try {
            // Use AI to generate the build
            AIBuildRequest aiRequest = new AIBuildRequest(
                    build.getBudget() != null ? build.getBudget().doubleValue() : null,
                    requirements
            );

            AIBuildResponse aiResponse = aiBuildService.generateAIBuild(aiRequest);

            if (aiResponse.isSuccess() && aiResponse.getComponentIds() != null) {
                // Set the component IDs from AI response
                setComponentIdsFromAI(build, aiResponse.getComponentIds());

                // Calculate total price
                BigDecimal totalPrice = calculateTotalPrice(aiResponse.getComponentIds());
                build.setTotalPrice(totalPrice);

                System.out.println("AI build generation successful");
            } else {
                // Fallback to simple selection if AI fails
                System.err.println("AI generation failed, using fallback: " + aiResponse.getMessage());
                fallbackBuildGeneration(build, requirements);
            }

        } catch (Exception e) {
            System.err.println("Error in AI build generation, using fallback: " + e.getMessage());
            fallbackBuildGeneration(build, requirements);
        }

        return build;
    }

    private void setComponentIdsFromAI(Build build, Map<String, Long> componentIds) {
        build.setCpuId(componentIds.get("cpuId"));
        build.setGpuId(componentIds.get("gpuId"));
        build.setPsuId(componentIds.get("psuId"));
        build.setRamId(componentIds.get("ramId"));
        build.setStorageId(componentIds.get("storageId"));
        build.setMotherboardId(componentIds.get("motherboardId"));
        build.setCaseId(componentIds.get("caseId"));
    }

    private BigDecimal calculateTotalPrice(Map<String, Long> componentIds) {
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (Long componentId : componentIds.values()) {
            if (componentId != null) {
                componentRepository.findById(componentId)
                        .ifPresent(component -> {
                            if (component.getPrice() != null) {
                                totalPrice.add(component.getPrice());
                            }
                        });
            }
        }

        return totalPrice;
    }

    /**
     * Fallback method if AI generation fails
     */
    private void fallbackBuildGeneration(Build build, Map<String, Map<String, Object>> requirements) {
        BigDecimal totalPrice = BigDecimal.ZERO;

        // Simple component selection as fallback
        for (Map.Entry<String, Map<String, Object>> entry : requirements.entrySet()) {
            String componentType = entry.getKey();

            // Get first available component of this type
            componentRepository.findByType(PcComponent.ComponentType.valueOf(componentType))
                    .stream()
                    .findFirst()
                    .ifPresent(component -> {
                        setComponentId(build, componentType, component.getId());
                        if (component.getPrice() != null) {
                            totalPrice.add(component.getPrice());
                        }
                    });
        }

        build.setTotalPrice(totalPrice);
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