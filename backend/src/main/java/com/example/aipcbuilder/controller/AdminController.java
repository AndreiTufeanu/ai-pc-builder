package com.example.aipcbuilder.controller;

import com.example.aipcbuilder.dto.ChatRequest;
import com.example.aipcbuilder.dto.ChatResponse;
import com.example.aipcbuilder.model.ChatMessage;
import com.example.aipcbuilder.model.PcComponent;
import com.example.aipcbuilder.repository.PcComponentRepository;
import com.example.aipcbuilder.service.ChatMessageService;
import com.example.aipcbuilder.service.ChromaDBService;
import com.example.aipcbuilder.service.PCBuilderService;
import com.example.aipcbuilder.utils.ComponentUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final PcComponentRepository componentRepository;
    private final PCBuilderService pcBuilderService;
    private final ChromaDBService chromaDBService;
    private final ChatMessageService chatMessageService;
    private final ComponentUtils componentUtils;

    public AdminController(PcComponentRepository componentRepository,
                           PCBuilderService pcBuilderService,
                           ChromaDBService chromaDBService,
                           ChatMessageService chatMessageService,
                           ComponentUtils componentUtils) {
        this.componentRepository = componentRepository;
        this.pcBuilderService = pcBuilderService;
        this.chromaDBService = chromaDBService;
        this.chatMessageService = chatMessageService;
        this.componentUtils = componentUtils;
    }

    @GetMapping("/components")
    public ResponseEntity<List<PcComponent>> getAllComponents() {
        try {
            List<PcComponent> components = componentRepository.findAllByOrderByNameAsc();
            System.out.println("Found " + components.size() + " components in database");
            return ResponseEntity.ok(components);
        } catch (Exception e) {
            System.err.println("Error fetching components: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/components")
    public ResponseEntity<?> addComponent(@RequestBody Map<String, Object> requestData) {
        try {
            System.out.println("Received component data: " + requestData);

            if (!componentUtils.isValidComponentRequest(requestData)) {
                return componentUtils.badRequest("Component name and type are required");
            }

            PcComponent component = componentUtils.createComponentFromRequest(requestData);
            PcComponent savedComponent = componentRepository.save(component);

            System.out.println("Component saved with ID: " + savedComponent.getId());
            chromaDBService.syncComponent(savedComponent);

            return ResponseEntity.ok(savedComponent);

        } catch (Exception e) {
            System.err.println("Error adding component: " + e.getMessage());
            return componentUtils.internalServerError("Error adding component: " + e.getMessage());
        }
    }

    @PutMapping("/components/{id}")
    public ResponseEntity<?> updateComponent(@PathVariable Long id, @RequestBody Map<String, Object> requestData) {
        try {
            System.out.println("Updating component with ID: " + id);

            return componentRepository.findById(id)
                    .map(existingComponent -> {
                        componentUtils.updateComponentFromRequest(existingComponent, requestData);
                        PcComponent updatedComponent = componentRepository.save(existingComponent);

                        System.out.println("Component updated successfully: " + updatedComponent.getName());
                        chromaDBService.syncComponent(updatedComponent);

                        return ResponseEntity.ok(updatedComponent);
                    })
                    .orElse(ResponseEntity.notFound().build());

        } catch (Exception e) {
            System.err.println("Error updating component: " + e.getMessage());
            return componentUtils.internalServerError("Error updating component: " + e.getMessage());
        }
    }

    @DeleteMapping("/components/{id}")
    public ResponseEntity<?> deleteComponent(@PathVariable Long id) {
        try {
            if (!componentRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }

            chromaDBService.deleteComponent(id);
            componentRepository.deleteById(id);

            System.out.println("Component deleted successfully: " + id);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            System.err.println("Error deleting component: " + e.getMessage());
            return componentUtils.internalServerError("Error deleting component: " + e.getMessage());
        }
    }

    @GetMapping("/components/{id}")
    public ResponseEntity<PcComponent> getComponentById(@PathVariable Long id) {
        try {
            return componentRepository.findById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            System.err.println("Error fetching component: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> adminChat(@RequestBody ChatRequest request) {
        System.out.println("Admin chat message from user ID: " + request.getUserId() + ", Message: " + request.getMessage());

        String response = pcBuilderService.getAdminTrainingResponse(request.getMessage());
        ChatMessage savedMessage = chatMessageService.saveChatMessage(request.getUserId(), request.getMessage(), response);

        System.out.println("Admin chat message saved with ID: " + savedMessage.getId());
        return ResponseEntity.ok(new ChatResponse(response, savedMessage.getId()));
    }
}