package com.example.aipcbuilder.service.build.helper;

import com.example.aipcbuilder.model.PcComponent;
import com.example.aipcbuilder.repository.PcComponentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class ComponentSelectorService {

    private final PcComponentRepository componentRepository;

    public PcComponent findComponentById(String componentId, String componentType) {
        if (componentId == null || componentId.trim().isEmpty()) {
            log.warn("Empty component ID for type: {}", componentType);
            return null;
        }
        try {
            Long id = Long.parseLong(componentId.trim());
            Optional<PcComponent> component = componentRepository.findById(id);
            if (component.isPresent()) {
                log.debug("Found component ID {} for type {}", id, componentType);
                return component.get();
            }
            log.error("Could not find component in database with ID: {} for type: {}", componentId, componentType);
            return null;
        } catch (NumberFormatException e) {
            log.error("Invalid component ID format: {} for type: {}", componentId, componentType);
            return null;
        } catch (Exception e) {
            log.error("Error finding component by ID: {}", e.getMessage());
            return null;
        }
    }

    public String parseComponentIdFromResponse(String response, String componentType) {
        if (response == null) return null;

        String cleanResponse = response.replaceAll("[\"{}]", "").trim();
        String idOnly = cleanResponse.replaceAll("[^0-9]", "").trim();

        if (!idOnly.isEmpty()) {
            log.debug("Parsed component ID: '{}' from response: '{}'", idOnly, cleanResponse);
            return idOnly;
        }

        log.error("Could not parse component ID from response: {} for type: {}", cleanResponse, componentType);
        return null;
    }

    public String buildComponentSpecificQuery(String componentType,
                                              Map<String, Map<String, Object>> requirements,
                                              Map<String, PcComponent> alreadySelected,
                                              double remainingBudget,
                                              int iteration) {
        StringBuilder query = new StringBuilder(componentType).append(" ");
        addCompatibilityKeywords(query, componentType, alreadySelected);
        if (iteration > 1) query.append("budget cost-effective ");
        query.append("Budget $").append(remainingBudget).append(" ");

        if (requirements != null && requirements.containsKey(componentType.toUpperCase())) {
            @SuppressWarnings("unchecked")
            Map<String, Object> specs = (Map<String, Object>) requirements.get(componentType.toUpperCase()).get("specifications");
            if (specs != null) specs.values().stream().filter(Objects::nonNull).forEach(v -> query.append(v).append(" "));
        }

        String finalQuery = query.toString().trim();
        log.debug("Built query for {}: {}", componentType, finalQuery);
        return finalQuery;
    }

    private void addCompatibilityKeywords(StringBuilder query, String componentType, Map<String, PcComponent> alreadySelected) {
        Map<String, Integer> sizeRank = Map.of("E-ATX", 4, "ATX", 3, "MATX", 2, "ITX", 1);
        Map<String, String> ramTypes = Map.of("AM4", "DDR4", "AM5", "DDR5", "LGA1851", "DDR5", "LGA1700", "DDR4 DDR5");
        String socket;
        switch (componentType.toUpperCase()) {
            case "PSU":
                if (alreadySelected.containsKey("gpu")) {
                    String connectors = getSpec(alreadySelected.get("gpu"), "powerConnectors");
                    String gpuTdp = getSpec(alreadySelected.get("gpu"), "tdp");
                    String cpuTdp = getSpec(alreadySelected.get("cpu"), "tdp");
                    if (connectors != null) query.append(connectors).append(" ");
                    if (gpuTdp != null && cpuTdp != null) query.append(estimateTotalPower(gpuTdp, cpuTdp)).append("W ");
                }
                break;
            case "RAM":
                socket = getSpec(alreadySelected.get("cpu"), "socket");
                if (socket != null && ramTypes.containsKey(socket.toUpperCase())) query.append(ramTypes.get(socket.toUpperCase())).append(" ");
                break;
            case "MOTHERBOARD":
                if ((socket = getSpec(alreadySelected.get("cpu"), "socket")) != null) query.append(socket).append(" ");
                break;
            case "CASE":
                String caseFormFactor = getRequiredCaseFormFactor(alreadySelected, sizeRank);
                if (caseFormFactor != null) query.append(caseFormFactor).append(" ");
                if (alreadySelected.containsKey("gpu")) query.append("large ");
                break;
        }
    }

    private String getRequiredCaseFormFactor(Map<String, PcComponent> alreadySelected, Map<String, Integer> sizeRank) {
        String mobo = getSpec(alreadySelected.get("motherboard"), "formFactor");
        String psu = getSpec(alreadySelected.get("psu"), "formFactor");

        if (mobo != null && psu != null) {
            int moboRank = sizeRank.getOrDefault(mobo.toUpperCase(), 2);
            int psuRank = psu.equalsIgnoreCase("ATX") ? 2 : 1;
            int requiredRank = Math.max(moboRank, psuRank);

            return switch (requiredRank) {
                case 4 -> "E-ATX"; case 3 -> "ATX"; case 2 -> "mATX"; case 1 -> "ITX"; default -> "ATX";
            };
        }
        return mobo;
    }

    private String getSpec(PcComponent component, String specName) {
        if (component == null || component.getSpecifications() == null) return null;
        Object spec = component.getSpecifications().get(specName);
        return spec != null ? spec.toString() : null;
    }

    private int estimateTotalPower(String gpuTdp, String cpuTdp) {
        try {
            int gpuW = Integer.parseInt(gpuTdp.replaceAll("[^0-9]", ""));
            int cpuW = Integer.parseInt(cpuTdp.replaceAll("[^0-9]", ""));
            int estimatedPower = (int)((gpuW + cpuW + 150) * 1.3);
            log.debug("Estimated total power: {}W (GPU: {}W, CPU: {}W)", estimatedPower, gpuW, cpuW);
            return estimatedPower;
        } catch (NumberFormatException e) {
            log.warn("Failed to parse power values, defaulting to 750W");
            return 750;
        }
    }
}