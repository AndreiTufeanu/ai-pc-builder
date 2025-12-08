package com.example.aipcbuilder.service.build.helper;

import com.example.aipcbuilder.model.PcComponent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PromptBuilderService {

    private final List<String> COMPONENT_PRIORITY = List.of(
            "CPU", "GPU", "PSU", "RAM", "MOTHERBOARD", "STORAGE", "CASE"
    );

    public String buildComponentSelectionPrompt(String componentType, String context, int iteration, Map<String, PcComponent> alreadySelected) {
        String compatibilityRules = buildCompatibilityRules(componentType);

        if (iteration == 1) {
            return buildInitialSelectionPrompt(componentType, context, compatibilityRules, alreadySelected);
        } else {
            return buildRefinementPrompt(componentType, context, compatibilityRules, alreadySelected);
        }
    }

    private String buildInitialSelectionPrompt(String componentType, String context, String compatibilityRules, Map<String, PcComponent> alreadySelected) {
        String selectedComponents = buildAlreadySelectedContext(alreadySelected);
        return """
            You are an expert PC building AI assistant. Your task is to select the best %s from the available options.
            
            CRITICAL INSTRUCTIONS:
            1. You MUST select ONLY from the provided component list below.
            2. **You CANNOT use components that are not in this list. Your database only contains these components.**
            3. Choose the component that best matches the user's requirements and budget.
            4. **CRITICALLY ENSURE COMPATIBILITY with already selected components using these rules:**
            %s
            5. You MUST return ONLY the component ID (the number in [ID: ...]) from the list.
            6. **NEVER return multiple component IDs or list multiple options.**
            7. Do NOT return the component name or any other text.
            8. **CRITICAL: Your response must contain ONLY the component ID, NO other text, NO explanations, NO reasoning.**
            
            %s
            
            ===== AVAILABLE %s OPTIONS =====
            %s
            
            ===== OUTPUT FORMAT =====
            Return ONLY the component ID from the list above, nothing else.
            Example: "123"
            
            **ANY OTHER TEXT IN YOUR RESPONSE WILL CAUSE THE BUILD TO FAIL.**
            **YOU CANNOT USE COMPONENTS NOT IN THIS LIST.**
            """.formatted(componentType, compatibilityRules, selectedComponents, componentType, context);
    }

    private String buildRefinementPrompt(String componentType, String context, String compatibilityRules, Map<String, PcComponent> alreadySelected) {
        String selectedComponents = buildAlreadySelectedContext(alreadySelected);

        return """
            You are refining a PC build to reduce costs while maintaining compatibility and performance.
            
            CRITICAL INSTRUCTIONS:
            1. You MUST select ONLY from the provided component list below.
            2. Choose a more cost-effective alternative that maintains good performance.
            3. **CRITICALLY ENSURE COMPATIBILITY with already selected components using these rules:**
            %s
            4. Consider the budget feedback provided in the requirements.
            5. You MUST return ONLY the component ID (the number in [ID: ...]) from the list.
            6. **NEVER return multiple component IDs or list multiple options.**
            7. Do NOT return the component name or any other text.
            8. **CRITICAL: Your response must contain ONLY the component ID, NO other text, NO explanations, NO reasoning.**
            
            %s
            
            ===== AVAILABLE %s OPTIONS =====
            %s
            
            ===== OUTPUT FORMAT =====
            Return ONLY the component ID from the list above, nothing else.
            
            **ANY OTHER TEXT IN YOUR RESPONSE WILL CAUSE THE BUILD TO FAIL.**
            """.formatted(compatibilityRules, selectedComponents, componentType, context);
    }

    private String buildAlreadySelectedContext(Map<String, PcComponent> alreadySelected) {
        if (alreadySelected.isEmpty()) {
            return "";
        }

        StringBuilder message = new StringBuilder();
        message.append("\nALREADY SELECTED COMPONENTS (ENSURE COMPATIBILITY WITH THESE SPECS):\n");
        for (Map.Entry<String, PcComponent> entry : alreadySelected.entrySet()) {
            if (entry.getValue() != null) {
                PcComponent component = entry.getValue();
                message.append("  - ").append(entry.getKey()).append(": [ID: ").append(component.getId())
                        .append("] ").append(component.getName())
                        .append(" ($").append(component.getPrice()).append(")\n");

                Map<String, Object> specs = component.getSpecifications();
                if (specs != null && !specs.isEmpty()) {
                    message.append("Specifications:\n");

                    for (Map.Entry<String, Object> spec : specs.entrySet()) {
                        if (spec.getValue() != null) {
                            message.append("      * ").append(spec.getKey()).append(": ").append(spec.getValue()).append("\n");
                        }
                    }
                }
                message.append("\n");
            }
        }

        return message.toString();
    }

    private String buildCompatibilityRules(String componentType) {
        return switch (componentType.toUpperCase()) {
            case "MOTHERBOARD" -> """
                    MOTHERBOARD COMPATIBILITY RULES:
                    - CPU Socket: Must match the selected CPU socket exactly (LGA1851, LGA1700, AM5, AM4)
                    - Form Factor: Must fit in the selected case (E-ATX, ATX, mATX, ITX)
                    """;
            case "CPU" -> """
                    CPU COMPATIBILITY RULES:
                    - Socket: Must match the motherboard socket exactly
                    - RAM Compatibility:
                          * LGA1851 and AM5 sockets support ONLY DDR5
                          * LGA1700 sockets support both DDR4 and DDR5
                          * AM4 sockets support ONLY DDR4
                    """;
            case "RAM" -> """
                    RAM COMPATIBILITY RULES:
                          Input: CPU Socket = AM5 → Output: RAM DDR5
                          Input: CPU Socket = AM4 → Output: RAM DDR4
                          Input: CPU Socket = LGA1851 → Output: RAM DDR5
                          Input: CPU Socket = LGA1700 → Output: RAM DDR4 | RAM DDR5
                    """;
            case "GPU" -> """
                    GPU COMPATIBILITY RULES:
                    - Case Fit: Must physically fit in case (check length)
                    - Power Supply: Must have required connectors (1 x 8-pin, 2 x 8-pin, 3 x 8-pin or 12VHPWR (16-pin))
                    """;
            case "PSU" -> """
                    PSU COMPATIBILITY RULES:
                    - Wattage: Must provide sufficient power for all components (GPU and CPU are the main components when calculating). Make sure you add some headroom (20-30% more than estimated total power draw).
                    - Connectors: Must have required connectors by the GPU (1 x 8-pin, 2 x 8-pin, 3 x 8-pin or 12VHPWR (16-pin))
                    - Form Factor: Must fit in case:
                          * E-ATX, ATX, and mATX cases support both ATX and SFX PSUs
                          * ITX cases support only SFX PSUs
                    - Efficiency: Higher efficiency (80+ Silver, 80+ Gold, 80+ Platinum, 80+ Titanium) for better power delivery, if the budget allows it
                    - Modern Standards: ATX 3.1 is always preferred for GPUs with 12VHPWR connectors and high tdp (350W+). ATX 3.0 alternatives are fine if any of these 2 conditions are not met.
                    """;
            case "CASE" -> """
                    CASE COMPATIBILITY RULES:
                    - Motherboard Form Factor: Must support motherboard size (E-ATX, ATX, mATX, ITX)
                    - GPU Length: Must accommodate GPU length with clearance
                    - PSU Form Factor Support:
                          * E-ATX, ATX, and mATX cases: Support both ATX and SFX PSUs
                          * ITX cases: Support only SFX PSUs
                    """;
            case "STORAGE" -> """
                    STORAGE COMPATIBILITY RULES:
                    - Nothing specific, all storage types are generally compatible with motherboards
                    """;
            default -> "Ensure compatibility with all selected components based on their specifications.";
        };
    }

    public String buildComponentSelectionMessage(String componentType,
                                                 Map<String, Map<String, Object>> requirements,
                                                 double remainingBudget,
                                                 int iteration,
                                                 List<String> remainingComponents) {
        StringBuilder message = new StringBuilder();

        message.append("=== BUILD PROGRESS ===\n");
        message.append("Currently selecting: ").append(componentType).append("\n");

        if (!remainingComponents.isEmpty()) {
            message.append("Components remaining: ").append(String.join(" → ", remainingComponents)).append("\n");
        } else {
            message.append("This is the final component selection.\n");
        }
        message.append("\n");

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
            @SuppressWarnings("unchecked")
            Map<String, Object> specs = (Map<String, Object>) componentReqs.get("specifications");
            if (specs != null && !specs.isEmpty()) {
                message.append("Specific Requirements:\n");
                for (Map.Entry<String, Object> spec : specs.entrySet()) {
                    message.append("  - ").append(spec.getKey()).append(": ").append(spec.getValue()).append("\n");
                }
            }
        }

        message.append("\nRemaining budget: $").append(remainingBudget);
        message.append("\n\nReturn only the component ID from the available options.");

        return message.toString();
    }

    public String buildRefinementInstruction(double overspentAmount) {
        StringBuilder instruction = new StringBuilder();

        instruction.append("The current build exceeds the budget by $").append(String.format("%.2f", overspentAmount))
                .append(". Please refine the component selection to reduce costs while maintaining compatibility.\n\n");

        instruction.append("Cost Reduction Strategy:\n");

        if (overspentAmount <= 50) {
            instruction.append("- Very small overspend: Make minimal adjustments, focus on finding slightly cheaper alternatives for 1-2 components\n");
            instruction.append("- Look for sales, bundles, or alternative brands with similar performance\n");
        } else if (overspentAmount <= 100) {
            instruction.append("- Small overspend: Make gradual adjustments across 2-3 components\n");
            instruction.append("- Focus on: Case → Storage → Motherboard → RAM\n");
            instruction.append("- Look for minor downgrades or alternative models with similar performance\n");
        } else if (overspentAmount <= 200) {
            instruction.append("- Moderate overspend: Reduce costs across multiple components\n");
            instruction.append("- Prioritize: Case → Storage → Motherboard → RAM → PSU\n");
            instruction.append("- Consider mid-range alternatives for some components\n");
        } else {
            instruction.append("- Significant overspend: Major cost reduction needed\n");
            instruction.append("- Re-evaluate all components, especially GPU and CPU\n");
            instruction.append("- Look for performance-efficient alternatives at lower price points\n");
        }

        instruction.append("\nTarget Reduction: Aim to reduce total cost by approximately $").append(String.format("%.2f", overspentAmount))
                .append(" (the exact overspent amount)\n\n");

        instruction.append("\nRemember: Compatibility is the highest priority. Only suggest compatible alternatives.");

        return instruction.toString();
    }

    public String buildChatSystemPrompt(String context) {
        String compatibilityRules = COMPONENT_PRIORITY.stream()
                .map(this::buildCompatibilityRules)
                .collect(Collectors.joining("\n\n", "===== ALL COMPONENT COMPATIBILITY RULES =====\n\n", "\n\n"));

        return """
            You are an expert PC building assistant. Help users choose compatible PC components.
            When suggesting parts, be specific about compatibility requirements.
            Keep responses concise and helpful.
            
            %s
            
            Available Components, Knowledge, and Conversation Context:
            %s
            
            Instructions:
            - Use the component information above when relevant
            - Consider compatibility between components
            - Suggest specific components when appropriate
            - Reference previous conversation context when relevant
            - If you don't have information, say so
            """.formatted(compatibilityRules, context);
    }

    public String buildAdminTrainingSystemPrompt(String context) {
        return """
            You are receiving training information about PC components and building.
            Acknowledge the information and explain how it will help improve recommendations.
            
            %s
            
            Respond professionally and thank the admin for the training data.
            """.formatted(context);
    }
}