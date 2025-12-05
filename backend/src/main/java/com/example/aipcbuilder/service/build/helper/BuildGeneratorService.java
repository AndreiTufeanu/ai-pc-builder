package com.example.aipcbuilder.service.build.helper;

import com.example.aipcbuilder.dto.AIBuildRequest;
import com.example.aipcbuilder.dto.AIBuildResponse;
import com.example.aipcbuilder.model.PcComponent;
import com.example.aipcbuilder.service.chroma.ChromaDBService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class BuildGeneratorService {

    private final ChatClient chatClient;
    private final ChromaDBService chromaDBService;
    private final ContextBuilderService contextBuilderService;
    private final PromptBuilderService promptBuilderService;
    private final ComponentSelectorService componentSelectorService;
    private final BuildResultMapperService buildResultMapperService;

    private final String[] COMPONENT_ORDER = {"CPU", "MOTHERBOARD", "RAM", "GPU", "STORAGE", "PSU", "CASE"};

    public BuildGeneratorService(ChatModel chatModel, ChromaDBService chromaDBService,
                                 ContextBuilderService contextBuilderService, PromptBuilderService promptBuilderService,
                                 ComponentSelectorService componentSelectorService, BuildResultMapperService buildResultMapperService) {
        this.chatClient = ChatClient.create(chatModel);
        this.chromaDBService = chromaDBService;
        this.contextBuilderService = contextBuilderService;
        this.promptBuilderService = promptBuilderService;
        this.componentSelectorService = componentSelectorService;
        this.buildResultMapperService = buildResultMapperService;
    }

    public AIBuildResponse generateAIBuild(AIBuildRequest request) {
        log.info("=== AI Build Generation Started ===");
        log.info("Budget: ${}", request.getBudget());
        log.info("Requirements: {}", request.getRequirements());

        Map<String, PcComponent> selectedComponents = new HashMap<>();
        int maxIterations = 3;

        for (int iteration = 1; iteration <= maxIterations; iteration++) {
            log.info("=== Iteration {} ===", iteration);

            selectedComponents = generateBuildIteration(request, selectedComponents, iteration);
            double totalPrice = buildResultMapperService.calculateTotalPrice(selectedComponents);
            double remainingBudget = request.getBudget() - totalPrice;

            log.info("Total price: ${}", totalPrice);
            log.info("Remaining budget: ${}", remainingBudget);

            if (remainingBudget >= 0) {
                log.info("✓ Build within budget!");
                Map<String, Long> componentIds = buildResultMapperService.convertComponentsToIdMap(selectedComponents);
                return new AIBuildResponse(componentIds, "Build generated successfully within budget", true, "Success");
            }

            if (iteration == maxIterations) {
                log.warn("✗ Max iterations reached, returning best effort build");
                break;
            }

            double overspentAmount = -remainingBudget;
            log.info("Build exceeds budget by ${}, starting refinement iteration...", overspentAmount);
            request = createRefinementRequest(request, selectedComponents, overspentAmount, totalPrice);
        }

        Map<String, Long> componentIds = buildResultMapperService.convertComponentsToIdMap(selectedComponents);
        return new AIBuildResponse(componentIds, "Build generated but exceeds budget after refinements", true, "Best effort - budget exceeded");
    }

    private Map<String, PcComponent> generateBuildIteration(AIBuildRequest request,
                                                            Map<String, PcComponent> previousSelection,
                                                            int iteration) {
        Map<String, PcComponent> selectedComponents = new HashMap<>();
        double remainingBudget = request.getBudget() != null ? request.getBudget() : Double.MAX_VALUE;

        for (String componentType : COMPONENT_ORDER) {
            log.info("=== Selecting {} (Iteration {}) ===", componentType, iteration);
            log.debug("Remaining budget: ${}", remainingBudget);

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
                log.info("Selected: {} (${})", selectedComponent.getName(), selectedComponent.getPrice());
            } else {
                log.warn("Could not find suitable {}", componentType);
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
        budgetFeedback.put("instruction", promptBuilderService.buildRefinementInstruction(overspentAmount, currentBuild));

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
            String searchQuery = componentSelectorService.buildComponentSpecificQuery(
                    componentType, requirements, alreadySelected, remainingBudget, iteration);

            List<Map<String, Object>> componentResults = chromaDBService.searchComponents(searchQuery, 5, componentType);
            List<Map<String, Object>> knowledgeResults = chromaDBService.searchAdminKnowledge(searchQuery, 3);

            if (componentResults.isEmpty()) {
                log.warn("No components found for {}", componentType);
                return null;
            }

            String context = buildSelectionContext(componentResults, knowledgeResults);
            String systemPrompt = promptBuilderService.buildComponentSelectionPrompt(
                    componentType, context, iteration, previousSelection);

            List<String> remainingComponents = getRemainingComponents(componentType);
            String userMessage = promptBuilderService.buildComponentSelectionMessage(
                    componentType, requirements, alreadySelected, remainingBudget, iteration, remainingComponents);

            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call()
                    .content();

            log.debug("{} selection response: {}", componentType, response);
            String componentId = componentSelectorService.parseComponentIdFromResponse(response, componentType);

            return componentSelectorService.findComponentById(componentId, componentType);

        } catch (Exception e) {
            log.error("Error selecting {}: {}", componentType, e.getMessage());
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
            context.append(contextBuilderService.buildComponentContext(componentResults));
        }

        if (!knowledgeResults.isEmpty()) {
            context.append(contextBuilderService.buildKnowledgeContext(knowledgeResults));
        }

        return context.toString();
    }
}