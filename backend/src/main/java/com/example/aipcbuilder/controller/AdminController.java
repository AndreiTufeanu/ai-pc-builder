package com.example.aipcbuilder.controller;

import com.example.aipcbuilder.dto.ChatRequest;
import com.example.aipcbuilder.dto.ChatResponse;
import com.example.aipcbuilder.model.ChatMessage;
import com.example.aipcbuilder.model.PcComponent;
import com.example.aipcbuilder.service.chat.ChatMessageService;
import com.example.aipcbuilder.service.component.ComponentService;
import com.example.aipcbuilder.service.build.PCBuilderService;
import com.example.aipcbuilder.utils.ResponseHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AdminController {

    private final ComponentService componentService;
    private final PCBuilderService pcBuilderService;
    private final ChatMessageService chatMessageService;
    private final ResponseHelper responseHelper;

    @GetMapping("/components")
    public ResponseEntity<List<PcComponent>> getAllComponents() {
        try {
            List<PcComponent> components = componentService.getAllComponents();
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

            PcComponent savedComponent = componentService.createComponent(requestData);
            System.out.println("Component saved with ID: " + savedComponent.getId());

            return ResponseEntity.ok(savedComponent);

        } catch (IllegalArgumentException e) {
            return responseHelper.badRequest(e.getMessage());
        } catch (Exception e) {
            System.err.println("Error adding component: " + e.getMessage());
            return responseHelper.internalServerError("Error adding component: " + e.getMessage());
        }
    }

    @PutMapping("/components/{id}")
    public ResponseEntity<?> updateComponent(@PathVariable Long id, @RequestBody Map<String, Object> requestData) {
        try {
            System.out.println("Updating component with ID: " + id);

            return componentService.updateComponent(id, requestData)
                    .map(updatedComponent -> {
                        System.out.println("Component updated successfully: " + updatedComponent.getName());
                        return ResponseEntity.ok(updatedComponent);
                    })
                    .orElse(ResponseEntity.notFound().build());

        } catch (Exception e) {
            System.err.println("Error updating component: " + e.getMessage());
            return responseHelper.internalServerError("Error updating component: " + e.getMessage());
        }
    }

    @DeleteMapping("/components/{id}")
    public ResponseEntity<?> deleteComponent(@PathVariable Long id) {
        try {
            boolean deleted = componentService.deleteComponent(id);
            if (!deleted) {
                return ResponseEntity.notFound().build();
            }

            System.out.println("Component deleted successfully: " + id);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            System.err.println("Error deleting component: " + e.getMessage());
            return responseHelper.internalServerError("Error deleting component: " + e.getMessage());
        }
    }

    @GetMapping("/components/{id}")
    public ResponseEntity<PcComponent> getComponentById(@PathVariable Long id) {
        try {
            return componentService.getComponentById(id)
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