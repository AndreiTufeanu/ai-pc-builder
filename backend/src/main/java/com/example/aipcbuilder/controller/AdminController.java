package com.example.aipcbuilder.controller;

import com.example.aipcbuilder.dto.ChatRequest;
import com.example.aipcbuilder.dto.ChatResponse;
import com.example.aipcbuilder.model.ChatMessage;
import com.example.aipcbuilder.model.PcComponent;
import com.example.aipcbuilder.service.chat.ChatMessageService;
import com.example.aipcbuilder.service.component.ComponentService;
import com.example.aipcbuilder.service.build.PCBuilderService;
import com.example.aipcbuilder.utils.ResponseHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final ComponentService componentService;
    private final PCBuilderService pcBuilderService;
    private final ChatMessageService chatMessageService;
    private final ResponseHelper responseHelper;

    @GetMapping("/components")
    public ResponseEntity<List<PcComponent>> getAllComponents() {
        try {
            List<PcComponent> components = componentService.getAllComponents();
            log.info("Found {} components in database", components.size());
            return ResponseEntity.ok(components);
        } catch (Exception e) {
            log.error("Error fetching components: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/components")
    public ResponseEntity<?> addComponent(@RequestBody Map<String, Object> requestData) {
        try {
            log.debug("Received component data: {}", requestData);

            PcComponent savedComponent = componentService.createComponent(requestData);
            log.info("Component saved with ID: {}", savedComponent.getId());

            return ResponseEntity.ok(savedComponent);

        } catch (IllegalArgumentException e) {
            return responseHelper.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("Error adding component: {}", e.getMessage(), e);
            return responseHelper.internalServerError("Error adding component: " + e.getMessage());
        }
    }

    @PutMapping("/components/{id}")
    public ResponseEntity<?> updateComponent(@PathVariable Long id, @RequestBody Map<String, Object> requestData) {
        try {
            log.info("Updating component with ID: {}", id);

            return componentService.updateComponent(id, requestData)
                    .map(updatedComponent -> {
                        log.info("Component updated successfully: {}", updatedComponent.getName());
                        return ResponseEntity.ok(updatedComponent);
                    })
                    .orElse(ResponseEntity.notFound().build());

        } catch (Exception e) {
            log.error("Error updating component with ID {}: {}", id, e.getMessage(), e);
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

            log.info("Component deleted successfully: {}", id);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Error deleting component with ID {}: {}", id, e.getMessage(), e);
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
            log.error("Error fetching component with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> adminChat(@Valid @RequestBody ChatRequest request) {
        log.info("Admin chat message from user ID: {}, Message: {}",
                request.getUserId(), request.getMessage());

        String response = pcBuilderService.getAdminTrainingResponse(request.getMessage());
        ChatMessage savedMessage = chatMessageService.saveChatMessage(request.getUserId(), request.getMessage(), response);

        log.info("Admin chat message saved with ID: {}", savedMessage.getId());
        return ResponseEntity.ok(new ChatResponse(response, savedMessage.getId()));
    }
}