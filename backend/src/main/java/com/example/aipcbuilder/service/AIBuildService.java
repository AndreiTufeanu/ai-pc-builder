package com.example.aipcbuilder.service;

import com.example.aipcbuilder.dto.AIBuildRequest;
import com.example.aipcbuilder.dto.AIBuildResponse;
import com.example.aipcbuilder.model.PcComponent;
import com.example.aipcbuilder.repository.PcComponentRepository;
import com.example.aipcbuilder.service.helper.ContextBuilderHelper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AIBuildService {

    private final ChatClient chatClient;
    private final ChromaDBService chromaDBService;
    private final PcComponentRepository componentRepository;
    private final ContextBuilderHelper contextBuilder;

    public AIBuildService(ChatModel chatModel, ChromaDBService chromaDBService,
                          PcComponentRepository componentRepository, ContextBuilderHelper contextBuilder) {
        this.chatClient = ChatClient.create(chatModel);
        this.chromaDBService = chromaDBService;
        this.componentRepository = componentRepository;
        this.contextBuilder = contextBuilder;
    }

    public AIBuildResponse generateAIBuild(AIBuildRequest request) {
        System.out.println("=== AI Build Generation Started ===");
        System.out.println("Budget: " + request.getBudget());
        System.out.println("Requirements: " + request.getRequirements());

        try {
            Map<String, PcComponent> selectedComponents = new HashMap<>();
            double remainingBudget = request.getBudget() != null ? request.getBudget() : Double.MAX_VALUE;

            String[] componentOrder = {"CPU", "MOTHERBOARD", "RAM", "GPU", "STORAGE", "PSU", "CASE"};

            for (String componentType : componentOrder) {
                System.out.println("=== Selecting " + componentType + " ===");
                System.out.println("Remaining budget: $" + remainingBudget);

                PcComponent selectedComponent = selectComponentForType(
                        componentType,
                        request.getRequirements(),
                        selectedComponents,
                        remainingBudget
                );

                if (selectedComponent != null) {
                    selectedComponents.put(componentType.toLowerCase(), selectedComponent);
                    remainingBudget -= selectedComponent.getPrice().doubleValue();
                    System.out.println("Selected: " + selectedComponent.getName() + " ($" + selectedComponent.getPrice() + ")");
                } else {
                    System.out.println("Could not find suitable " + componentType);
                }
            }

            Map<String, Long> componentIds = convertComponentsToIdMap(selectedComponents);

            System.out.println("=== Final Build ===");
            System.out.println("Selected components: " + selectedComponents);
            System.out.println("Component IDs: " + componentIds);

            return new AIBuildResponse(componentIds, "Build generated successfully using AI", true, "Success");

        } catch (Exception e) {
            System.err.println("Error in AI build generation: " + e.getMessage());
            e.printStackTrace();
            return new AIBuildResponse(null, "AI generation failed", false, "Error: " + e.getMessage());
        }
    }

    private PcComponent selectComponentForType(String componentType,
                                               Map<String, Map<String, Object>> requirements,
                                               Map<String, PcComponent> alreadySelected,
                                               double remainingBudget) {
        try {
            String searchQuery = buildComponentSpecificQuery(componentType, requirements, alreadySelected, remainingBudget);

            List<Map<String, Object>> componentResults = chromaDBService.searchComponents(searchQuery, 5);
            List<Map<String, Object>> knowledgeResults = chromaDBService.searchAdminKnowledge(searchQuery, 3);

            if (componentResults.isEmpty()) {
                return null;
            }

            String context = buildSelectionContext(componentResults, knowledgeResults);
            String systemPrompt = buildComponentSelectionPrompt(componentType, context);
            String userMessage = buildComponentSelectionMessage(componentType, requirements, alreadySelected, remainingBudget);

            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call()
                    .content();

            System.out.println(componentType + " selection response: " + response);
            String componentName = parseComponentNameFromResponse(response, componentType);

            return findComponentByName(componentName, componentType);

        } catch (Exception e) {
            System.err.println("Error selecting " + componentType + ": " + e.getMessage());
            return null;
        }
    }

    private String buildSelectionContext(List<Map<String, Object>> componentResults,
                                         List<Map<String, Object>> knowledgeResults) {
        StringBuilder context = new StringBuilder();

        if (!componentResults.isEmpty()) {
            context.append("=== RELEVANT COMPONENTS ===\n");
            context.append(contextBuilder.buildComponentContext(componentResults));
        }

        if (!knowledgeResults.isEmpty()) {
            context.append(contextBuilder.buildKnowledgeContext(knowledgeResults));
        }

        return context.toString();
    }

    private PcComponent findComponentByName(String componentName, String componentType) {
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

    private Map<String, Long> convertComponentsToIdMap(Map<String, PcComponent> selectedComponents) {
        Map<String, Long> componentIds = new HashMap<>();
        for (Map.Entry<String, PcComponent> entry : selectedComponents.entrySet()) {
            String componentKey = entry.getKey();
            PcComponent component = entry.getValue();
            if (component != null) {
                switch (componentKey) {
                    case "cpu": componentIds.put("cpuId", component.getId()); break;
                    case "gpu": componentIds.put("gpuId", component.getId()); break;
                    case "psu": componentIds.put("psuId", component.getId()); break;
                    case "ram": componentIds.put("ramId", component.getId()); break;
                    case "storage": componentIds.put("storageId", component.getId()); break;
                    case "motherboard": componentIds.put("motherboardId", component.getId()); break;
                    case "case": componentIds.put("caseId", component.getId()); break;
                }
            }
        }
        return componentIds;
    }

    private String buildComponentSpecificQuery(String componentType,
                                               Map<String, Map<String, Object>> requirements,
                                               Map<String, PcComponent> alreadySelected,
                                               double remainingBudget) {
        StringBuilder query = new StringBuilder();
        query.append(componentType).append(" ");
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

    private String buildComponentSelectionPrompt(String componentType, String context) {
        return """
        You are an expert PC building AI assistant. Your task is to select the best %s from the available options.
        
        CRITICAL INSTRUCTIONS:
        1. You MUST select ONLY from the provided component list below.
        2. Choose the component that best matches the user's requirements and budget.
        3. Ensure compatibility with already selected components if specified.
        4. You MUST use the exact component name and model as it appears in the list.
        
        ===== AVAILABLE %s OPTIONS =====
        %s
        
        ===== OUTPUT FORMAT =====
        Return ONLY the exact component name from the list above, nothing else.
        Example: "AMD Ryzen 5 9600X"
        
        Do not add any other text or explanation.
        """.formatted(componentType, componentType, context);
    }

    private String buildComponentSelectionMessage(String componentType,
                                                  Map<String, Map<String, Object>> requirements,
                                                  Map<String, PcComponent> alreadySelected,
                                                  double remainingBudget) {
        StringBuilder message = new StringBuilder();
        message.append("Please select a ").append(componentType).append(" with these requirements:\n");

        if (requirements != null && requirements.containsKey(componentType.toUpperCase())) {
            Map<String, Object> componentReqs = requirements.get(componentType.toUpperCase());
            Map<String, Object> specs = (Map<String, Object>) componentReqs.get("specifications");
            if (specs != null && !specs.isEmpty()) {
                message.append("Requirements:\n");
                for (Map.Entry<String, Object> spec : specs.entrySet()) {
                    message.append("  - ").append(spec.getKey()).append(": ").append(spec.getValue()).append("\n");
                }
            }
        }

        if (!alreadySelected.isEmpty()) {
            message.append("\nAlready selected components (ensure compatibility):\n");
            for (Map.Entry<String, PcComponent> entry : alreadySelected.entrySet()) {
                if (entry.getValue() != null) {
                    message.append("  - ").append(entry.getKey()).append(": ").append(entry.getValue().getName())
                            .append(" ($").append(entry.getValue().getPrice()).append(")\n");
                }
            }
        }

        message.append("\nRemaining budget: $").append(remainingBudget);
        message.append("\n\nReturn only the exact component name from the available options.");
        return message.toString();
    }

    private String parseComponentNameFromResponse(String response, String componentType) {
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