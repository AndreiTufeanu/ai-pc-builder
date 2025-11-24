package com.example.aipcbuilder.service.helper;

import com.example.aipcbuilder.model.PcComponent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PromptBuilder {

    // Component priority for cost reduction
    private final List<String> COMPONENT_PRIORITY = List.of(
            "CPU", "GPU", "PSU", "RAM", "MOTHERBOARD", "STORAGE", "CASE"
    );

    public String buildComponentSelectionPrompt(String componentType, String context, int iteration, PcComponent previousSelection) {
        if (iteration == 1) {
            return buildInitialSelectionPrompt(componentType, context);
        } else {
            return buildRefinementPrompt(componentType, context, previousSelection);
        }
    }

    private String buildInitialSelectionPrompt(String componentType, String context) {
        return """
            You are an expert PC building AI assistant. Your task is to select the best %s from the available options.
            
            CRITICAL INSTRUCTIONS:
            1. You MUST select ONLY from the provided component list below.
            2. **You CANNOT use components that are not in this list. Your database only contains these components.**
            3. Choose the component that best matches the user's requirements and budget.
            4. Ensure compatibility with already selected components if specified.
            5. You MUST use the EXACT FULL component name exactly as it appears in the list.
            6. Do NOT shorten or modify the component names in any way.
            7. **CRITICAL: Your response must contain ONLY the component name, NO other text, NO explanations, NO reasoning.**
            
            ===== AVAILABLE %s OPTIONS =====
            %s
            
            ===== OUTPUT FORMAT =====
            Return ONLY the exact component name from the list above, nothing else.
            Example: "AMD Ryzen 5 9600X"
            
            **ANY OTHER TEXT IN YOUR RESPONSE WILL CAUSE THE BUILD TO FAIL.**
            **YOU CANNOT USE COMPONENTS NOT IN THIS LIST.**
            """.formatted(componentType, componentType, context);
    }

    private String buildRefinementPrompt(String componentType, String context, PcComponent previousSelection) {
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
            7. **CRITICAL: Your response must contain ONLY the component name, NO other text, NO explanations, NO reasoning.**
            
            %s
            
            ===== AVAILABLE %s OPTIONS =====
            %s
            
            ===== OUTPUT FORMAT =====
            Return ONLY the exact component name from the list above, nothing else.
            
            **ANY OTHER TEXT IN YOUR RESPONSE WILL CAUSE THE BUILD TO FAIL.**
            """.formatted(previousInfo, componentType, context);
    }

    public String buildComponentSelectionMessage(String componentType,
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

    public String buildRefinementInstruction(double overspentAmount, Map<String, PcComponent> currentBuild) {
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
}