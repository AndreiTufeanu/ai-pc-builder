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

    // Component priority for cost reduction (from highest to lowest impact on performance)
    private final List<String> COMPONENT_PRIORITY = Arrays.asList(
            "CPU", "GPU", "PSU", "RAM", "MOTHERBOARD", "STORAGE", "CASE"
    );

    public AIBuildService(ChatModel chatModel, ChromaDBService chromaDBService,
                          PcComponentRepository componentRepository, ContextBuilderHelper contextBuilder) {
        this.chatClient = ChatClient.create(chatModel);
        this.chromaDBService = chromaDBService;
        this.componentRepository = componentRepository;
        this.contextBuilder = contextBuilder;
    }

    public AIBuildResponse generateAIBuild(AIBuildRequest request) {
        System.out.println("=== AI Build Generation Started ===");
        System.out.println("Budget: $" + request.getBudget());
        System.out.println("Requirements: " + request.getRequirements());

        Map<String, PcComponent> selectedComponents = new HashMap<>();
        int maxIterations = 3;

        for (int iteration = 1; iteration <= maxIterations; iteration++) {
            System.out.println("=== Iteration " + iteration + " ===");

            selectedComponents = generateBuildIteration(request, selectedComponents, iteration);

            // Calculate total price
            double totalPrice = calculateTotalPrice(selectedComponents);
            double remainingBudget = request.getBudget() - totalPrice;

            System.out.println("Total price: $" + totalPrice);
            System.out.println("Remaining budget: $" + remainingBudget);

            // Check if we're within budget
            if (remainingBudget >= 0) {
                System.out.println("✓ Build within budget!");
                Map<String, Long> componentIds = convertComponentsToIdMap(selectedComponents);
                return new AIBuildResponse(componentIds, "Build generated successfully within budget", true, "Success");
            }

            // If this is the last iteration, break
            if (iteration == maxIterations) {
                System.out.println("✗ Max iterations reached, returning best effort build");
                break;
            }

            // Prepare for next iteration with budget feedback
            double overspentAmount = -remainingBudget;
            System.out.println("Build exceeds budget by $" + overspentAmount + ", starting refinement iteration...");

            // Add budget feedback to requirements for next iteration
            request = createRefinementRequest(request, selectedComponents, overspentAmount, totalPrice);
        }

        // Return the build even if it exceeds budget (best effort)
        Map<String, Long> componentIds = convertComponentsToIdMap(selectedComponents);
        return new AIBuildResponse(componentIds, "Build generated but exceeds budget after refinements", true, "Best effort - budget exceeded");
    }

    private Map<String, PcComponent> generateBuildIteration(AIBuildRequest request,
                                                            Map<String, PcComponent> previousSelection,
                                                            int iteration) {
        Map<String, PcComponent> selectedComponents = new HashMap<>();
        double remainingBudget = request.getBudget() != null ? request.getBudget() : Double.MAX_VALUE;

        String[] componentOrder = {"CPU", "MOTHERBOARD", "RAM", "GPU", "STORAGE", "PSU", "CASE"};

        for (String componentType : componentOrder) {
            System.out.println("=== Selecting " + componentType + " (Iteration " + iteration + ") ===");
            System.out.println("Remaining budget: $" + remainingBudget);

            PcComponent selectedComponent = selectComponentForType(
                    componentType,
                    request.getRequirements(),
                    selectedComponents,
                    remainingBudget,
                    previousSelection.get(componentType.toLowerCase()),
                    iteration
            );

            if (selectedComponent != null) {
                selectedComponents.put(componentType.toLowerCase(), selectedComponent);
                if (selectedComponent.getPrice() != null) {
                    remainingBudget -= selectedComponent.getPrice().doubleValue();
                }
                System.out.println("Selected: " + selectedComponent.getName() + " ($" + selectedComponent.getPrice() + ")");
            } else {
                System.out.println("Could not find suitable " + componentType);
            }
        }

        return selectedComponents;
    }

    private AIBuildRequest createRefinementRequest(AIBuildRequest originalRequest,
                                                   Map<String, PcComponent> currentBuild,
                                                   double overspentAmount,
                                                   double totalPrice) {
        // Create a new requirements map with budget feedback
        Map<String, Map<String, Object>> refinedRequirements = new HashMap<>();

        if (originalRequest.getRequirements() != null) {
            refinedRequirements.putAll(originalRequest.getRequirements());
        }

        // Add budget refinement instructions
        Map<String, Object> budgetFeedback = new HashMap<>();
        budgetFeedback.put("current_total_price", totalPrice);
        budgetFeedback.put("overspent_amount", overspentAmount);
        budgetFeedback.put("target_budget", originalRequest.getBudget());
        budgetFeedback.put("component_priority", COMPONENT_PRIORITY);
        budgetFeedback.put("instruction", buildRefinementInstruction(overspentAmount, currentBuild));

        refinedRequirements.put("BUDGET_FEEDBACK", budgetFeedback);

        return new AIBuildRequest(originalRequest.getBudget(), refinedRequirements);
    }

    private String buildRefinementInstruction(double overspentAmount, Map<String, PcComponent> currentBuild) {
        StringBuilder instruction = new StringBuilder();

        instruction.append("The current build exceeds the budget by $").append(String.format("%.2f", overspentAmount))
                .append(". Please refine the component selection to reduce costs while maintaining compatibility.\n\n");

        instruction.append("Cost Reduction Strategy:\n");

        if (overspentAmount <= 100) {
            instruction.append("- Small overspend: Focus on reducing costs in lower-priority components (Case, Storage, Motherboard, RAM)\n");
            instruction.append("- Look for minor downgrades or alternative models with similar performance but lower cost\n");
        } else if (overspentAmount <= 300) {
            instruction.append("- Moderate overspend: Reduce costs across multiple components\n");
            instruction.append("- Prioritize: Case → Storage → Motherboard → RAM → PSU → GPU → CPU\n");
            instruction.append("- Consider mid-range alternatives for higher-priority components\n");
        } else {
            instruction.append("- Significant overspend: Major cost reduction needed\n");
            instruction.append("- Re-evaluate all components, especially GPU and CPU\n");
            instruction.append("- Look for performance-efficient alternatives at lower price points\n");
        }

        instruction.append("\nCurrent Component Costs:\n");
        for (String priority : COMPONENT_PRIORITY) {
            String key = priority.toLowerCase();
            PcComponent component = currentBuild.get(key);
            if (component != null && component.getPrice() != null) {
                instruction.append("- ").append(priority).append(": $").append(component.getPrice())
                        .append(" (").append(component.getName()).append(")\n");
            }
        }

        instruction.append("\nRemember: Compatibility is the highest priority. Only suggest compatible alternatives.");

        return instruction.toString();
    }

    private PcComponent selectComponentForType(String componentType,
                                               Map<String, Map<String, Object>> requirements,
                                               Map<String, PcComponent> alreadySelected,
                                               double remainingBudget,
                                               PcComponent previousSelection,
                                               int iteration) {
        try {
            String searchQuery = buildComponentSpecificQuery(componentType, requirements, alreadySelected, remainingBudget, iteration);
            List<Map<String, Object>> componentResults = chromaDBService.searchComponents(searchQuery, 5);
            List<Map<String, Object>> knowledgeResults = chromaDBService.searchAdminKnowledge(searchQuery, 3);

            if (componentResults.isEmpty()) {
                return null;
            }

            String context = buildSelectionContext(componentResults, knowledgeResults);
            String systemPrompt = buildComponentSelectionPrompt(componentType, context, iteration, previousSelection);
            String userMessage = buildComponentSelectionMessage(componentType, requirements, alreadySelected, remainingBudget, iteration);

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

    private String buildComponentSelectionPrompt(String componentType, String context, int iteration, PcComponent previousSelection) {
        if (iteration == 1) {
            return """
            You are an expert PC building AI assistant. Your task is to select the best %s from the available options.
            
            CRITICAL INSTRUCTIONS:
            1. You MUST select ONLY from the provided component list below.
            2. Choose the component that best matches the user's requirements and budget.
            3. Ensure compatibility with already selected components if specified.
            4. You MUST use the EXACT FULL component name exactly as it appears in the list.
            5. Do NOT shorten or modify the component names in any way.
            
            ===== AVAILABLE %s OPTIONS =====
            %s
            
            ===== OUTPUT FORMAT =====
            Return ONLY the exact component name from the list above, nothing else.
            Example: "AMD Ryzen 5 9600X"
            
            Do not add any other text or explanation.
            """.formatted(componentType, componentType, context);
        } else {
            // Refinement iteration prompt
            String previousInfo = previousSelection != null ?
                    "Previous selection: " + previousSelection.getName() + " ($" + previousSelection.getPrice() + ")" :
                    "No previous selection";

            return """
            You are refining a PC build to reduce costs while maintaining compatibility and performance.
            
            CRITICAL INSTRUCTIONS:
            1. You MUST select ONLY from the provided component list below.
            2. Choose a more cost-effective alternative that maintains good performance.
            3. Ensure compatibility with already selected components.
            4. Consider the budget feedback provided in the requirements.
            5. You MUST use the EXACT FULL component name exactly as it appears in the list.
            6. Do NOT shorten or modify the component names in any way.
            
            %s
            
            ===== AVAILABLE %s OPTIONS =====
            %s
            
            ===== OUTPUT FORMAT =====
            Return ONLY the exact component name from the list above, nothing else.
            
            Do not add any other text or explanation.
            """.formatted(previousInfo, componentType, context);
        }
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

    private String buildComponentSelectionMessage(String componentType,
                                                  Map<String, Map<String, Object>> requirements,
                                                  Map<String, PcComponent> alreadySelected,
                                                  double remainingBudget,
                                                  int iteration) {
        StringBuilder message = new StringBuilder();

        if (iteration > 1) {
            message.append("REFINEMENT ITERATION: Please select a more cost-effective ").append(componentType).append("\n\n");
        } else {
            message.append("Please select a ").append(componentType).append(" with these requirements:\n");
        }

        // Add budget feedback from requirements if available
        if (requirements != null && requirements.containsKey("BUDGET_FEEDBACK")) {
            Map<String, Object> budgetFeedback = requirements.get("BUDGET_FEEDBACK");
            String instruction = (String) budgetFeedback.get("instruction");
            message.append(instruction).append("\n\n");
        }

        // Add specific requirements for this component
        if (requirements != null && requirements.containsKey(componentType.toUpperCase())) {
            Map<String, Object> componentReqs = requirements.get(componentType.toUpperCase());
            Map<String, Object> specs = (Map<String, Object>) componentReqs.get("specifications");
            if (specs != null && !specs.isEmpty()) {
                message.append("Specific Requirements:\n");
                for (Map.Entry<String, Object> spec : specs.entrySet()) {
                    message.append("  - ").append(spec.getKey()).append(": ").append(spec.getValue()).append("\n");
                }
            }
        }

        // Add compatibility constraints
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

    private double calculateTotalPrice(Map<String, PcComponent> selectedComponents) {
        double totalPrice = 0.0;
        for (PcComponent component : selectedComponents.values()) {
            if (component != null && component.getPrice() != null) {
                totalPrice += component.getPrice().doubleValue();
            }
        }
        return totalPrice;
    }
}