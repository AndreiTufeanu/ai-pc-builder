// BuildGenerationService.java
package com.example.aipcbuilder.service.build.helper;

import com.example.aipcbuilder.dto.AIBuildRequest;
import com.example.aipcbuilder.dto.AIBuildResponse;
import com.example.aipcbuilder.model.Build;
import com.example.aipcbuilder.model.PcComponent;
import com.example.aipcbuilder.repository.PcComponentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class BuildGenerationService {

    private final PcComponentRepository componentRepository;
    private final BuildGeneratorService buildGeneratorService;

    public Build generateBuild(Build build, Map<String, Map<String, Object>> requirements) {
        log.info("=== AI Build Generation ===");

        try {
            AIBuildRequest aiRequest = new AIBuildRequest(
                    build.getBudget() != null ? build.getBudget().doubleValue() : null,
                    requirements
            );

            AIBuildResponse aiResponse = buildGeneratorService.generateAIBuild(aiRequest);

            if (aiResponse.isSuccess() && aiResponse.getComponentIds() != null) {
                setComponentIdsFromAI(build, aiResponse.getComponentIds());

                BigDecimal totalPrice = calculateTotalPrice(aiResponse.getComponentIds());
                build.setTotalPrice(totalPrice);

                log.info("AI build generation successful");
            }

        } catch (Exception e) {
            log.error("Error in AI build generation, using fallback: {}", e.getMessage());
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
                Optional<PcComponent> componentOpt = componentRepository.findById(componentId);
                if (componentOpt.isPresent()) {
                    PcComponent component = componentOpt.get();
                    if (component.getPrice() != null) {
                        totalPrice = totalPrice.add(component.getPrice());
                    }
                }
            }
        }

        return totalPrice;
    }
}