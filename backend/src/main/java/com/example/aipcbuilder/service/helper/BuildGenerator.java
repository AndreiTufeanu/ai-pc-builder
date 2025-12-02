package com.example.aipcbuilder.service.helper;

import com.example.aipcbuilder.dto.AIBuildRequest;
import com.example.aipcbuilder.dto.AIBuildResponse;
import com.example.aipcbuilder.model.PcComponent;
import com.example.aipcbuilder.service.chroma.ChromaDBService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BuildGenerator {

    private final ChatClient chatClient;
    private final ChromaDBService chromaDBService;
    private final ContextBuilderHelper contextBuilder;
    private final PromptBuilder promptBuilder;
    private final ComponentSelector componentSelector;
    private final BuildResultMapper buildResultMapper;

    private final String[] COMPONENT_ORDER = {"CPU", "MOTHERBOARD", "RAM", "GPU", "STORAGE", "PSU", "CASE"};

    public BuildGenerator(ChatModel chatModel, ChromaDBService chromaDBService,
                          ContextBuilderHelper contextBuilder, PromptBuilder promptBuilder,
                          ComponentSelector componentSelector, BuildResultMapper buildResultMapper) {
        this.chatClient = ChatClient.create(chatModel);
        this.chromaDBService = chromaDBService;
        this.contextBuilder = contextBuilder;
        this.promptBuilder = promptBuilder;
        this.componentSelector = componentSelector;
        this.buildResultMapper = buildResultMapper;
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
            double totalPrice = buildResultMapper.calculateTotalPrice(selectedComponents);
            double remainingBudget = request.getBudget() - totalPrice;

            System.out.println("Total price: $" + totalPrice);
            System.out.println("Remaining budget: $" + remainingBudget);

            if (remainingBudget >= 0) {
                System.out.println("✓ Build within budget!");
                Map<String, Long> componentIds = buildResultMapper.convertComponentsToIdMap(selectedComponents);
                return new AIBuildResponse(componentIds, "Build generated successfully within budget", true, "Success");
            }

            if (iteration == maxIterations) {
                System.out.println("✗ Max iterations reached, returning best effort build");
                break;
            }

            double overspentAmount = -remainingBudget;
            System.out.println("Build exceeds budget by $" + overspentAmount + ", starting refinement iteration...");
            request = createRefinementRequest(request, selectedComponents, overspentAmount, totalPrice);
        }

        Map<String, Long> componentIds = buildResultMapper.convertComponentsToIdMap(selectedComponents);
        return new AIBuildResponse(componentIds, "Build generated but exceeds budget after refinements", true, "Best effort - budget exceeded");
    }

    private Map<String, PcComponent> generateBuildIteration(AIBuildRequest request,
                                                            Map<String, PcComponent> previousSelection,
                                                            int iteration) {
        Map<String, PcComponent> selectedComponents = new HashMap<>();
        double remainingBudget = request.getBudget() != null ? request.getBudget() : Double.MAX_VALUE;

        for (String componentType : COMPONENT_ORDER) {
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
        Map<String, Map<String, Object>> refinedRequirements = new HashMap<>();

        if (originalRequest.getRequirements() != null) {
            refinedRequirements.putAll(originalRequest.getRequirements());
        }

        Map<String, Object> budgetFeedback = new HashMap<>();
        budgetFeedback.put("current_total_price", totalPrice);
        budgetFeedback.put("overspent_amount", overspentAmount);
        budgetFeedback.put("target_budget", originalRequest.getBudget());
        budgetFeedback.put("instruction", promptBuilder.buildRefinementInstruction(overspentAmount, currentBuild));

        refinedRequirements.put("BUDGET_FEEDBACK", budgetFeedback);
        return new AIBuildRequest(originalRequest.getBudget(), refinedRequirements);
    }

    private PcComponent selectComponentForType(String componentType,
                                               Map<String, Map<String, Object>> requirements,
                                               Map<String, PcComponent> alreadySelected,
                                               double remainingBudget,
                                               PcComponent previousSelection,
                                               int iteration) {
        try {
            String searchQuery = componentSelector.buildComponentSpecificQuery(
                    componentType, requirements, alreadySelected, remainingBudget, iteration);

            List<Map<String, Object>> componentResults = chromaDBService.searchComponents(searchQuery, 5, componentType);
            List<Map<String, Object>> knowledgeResults = chromaDBService.searchAdminKnowledge(searchQuery, 3);

            if (componentResults.isEmpty()) {
                return null;
            }

            String context = buildSelectionContext(componentResults, knowledgeResults);
            String systemPrompt = promptBuilder.buildComponentSelectionPrompt(
                    componentType, context, iteration, previousSelection);

            List<String> remainingComponents = getRemainingComponents(componentType);
            String userMessage = promptBuilder.buildComponentSelectionMessage(
                    componentType, requirements, alreadySelected, remainingBudget, iteration, remainingComponents);

            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call()
                    .content();

            System.out.println(componentType + " selection response: " + response);
            String componentId = componentSelector.parseComponentIdFromResponse(response, componentType);

            return componentSelector.findComponentById(componentId, componentType);

        } catch (Exception e) {
            System.err.println("Error selecting " + componentType + ": " + e.getMessage());
            return null;
        }
    }

    private List<String> getRemainingComponents(String currentComponent) {
        List<String> remaining = new ArrayList<>();
        boolean foundCurrent = false;

        for (String component : COMPONENT_ORDER) {
            if (foundCurrent) {
                remaining.add(component);
            }
            if (component.equals(currentComponent)) {
                foundCurrent = true;
            }
        }
        return remaining;
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
}