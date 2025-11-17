package com.example.aipcbuilder.controller;

import com.example.aipcbuilder.dto.ChatRequest;
import com.example.aipcbuilder.dto.ChatResponse;
import com.example.aipcbuilder.model.PcComponent;
import com.example.aipcbuilder.repository.PcComponentRepository;
import com.example.aipcbuilder.service.ChromaDBService;
import com.example.aipcbuilder.service.PCBuilderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final PcComponentRepository componentRepository;
    private final PCBuilderService pcBuilderService;
    private final ChromaDBService chromaDBService;

    public AdminController(PcComponentRepository componentRepository, PCBuilderService pcBuilderService, ChromaDBService chromaDBService) {
        this.componentRepository = componentRepository;
        this.pcBuilderService = pcBuilderService;
        this.chromaDBService = chromaDBService;
    }

    /**
     * Get all components from database
     */
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

    /**
     * Add a new component to database
     */
    @PostMapping("/components")
    public ResponseEntity<?> addComponent(@RequestBody Map<String, Object> requestData) {
        try {
            System.out.println("Received component data: " + requestData);

            // Basic validation
            if (!requestData.containsKey("name") || requestData.get("name").toString().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("{\"message\": \"Component name is required\"}");
            }

            if (!requestData.containsKey("type")) {
                return ResponseEntity.badRequest().body("{\"message\": \"Component type is required\"}");
            }

            // Create new component entity
            PcComponent component = new PcComponent();
            component.setName(requestData.get("name").toString());
            component.setType(PcComponent.ComponentType.valueOf(requestData.get("type").toString()));

            if (requestData.containsKey("description")) {
                component.setDescription(requestData.get("description").toString());
            }

            // Handle price
            if (requestData.containsKey("price") && requestData.get("price") != null) {
                try {
                    Double priceValue = Double.parseDouble(requestData.get("price").toString());
                    component.setPriceFromDouble(priceValue);
                } catch (NumberFormatException e) {
                    component.setPrice(null);
                }
            }

            if (requestData.containsKey("manufacturer")) {
                component.setManufacturer(requestData.get("manufacturer").toString());
            }

            if (requestData.containsKey("model")) {
                component.setModel(requestData.get("model").toString());
            }

            // Handle specifications - now it's a Map directly
            if (requestData.containsKey("specifications")) {
                Object specsObj = requestData.get("specifications");
                if (specsObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> specifications = (Map<String, Object>) specsObj;
                    component.setSpecifications(specifications);
                }
            }

            // Save to database
            PcComponent savedComponent = componentRepository.save(component);
            System.out.println("Component saved with ID: " + savedComponent.getId());

            // Sync to chromadb
            chromaDBService.syncComponent(savedComponent);
            System.out.println("Component synced to ChromaDB: " + savedComponent.getName());

            return ResponseEntity.ok(savedComponent);

        } catch (Exception e) {
            System.err.println("Error adding component: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("{\"message\": \"Error adding component: " + e.getMessage() + "\"}");
        }
    }

    /**
     * Update an existing component
     */
    @PutMapping("/components/{id}")
    public ResponseEntity<?> updateComponent(@PathVariable Long id, @RequestBody Map<String, Object> requestData) {
        try {
            System.out.println("Updating component with ID: " + id);
            System.out.println("Update data: " + requestData);

            // Check if component exists
            Optional<PcComponent> existingComponentOpt = componentRepository.findById(id);
            if (existingComponentOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            PcComponent existingComponent = existingComponentOpt.get();

            // Update fields
            if (requestData.containsKey("name")) {
                existingComponent.setName(requestData.get("name").toString());
            }

            if (requestData.containsKey("type")) {
                existingComponent.setType(PcComponent.ComponentType.valueOf(requestData.get("type").toString()));
            }

            if (requestData.containsKey("description")) {
                existingComponent.setDescription(requestData.get("description").toString());
            }

            // Handle price
            if (requestData.containsKey("price")) {
                if (requestData.get("price") != null) {
                    try {
                        Double priceValue = Double.parseDouble(requestData.get("price").toString());
                        existingComponent.setPriceFromDouble(priceValue);
                    } catch (NumberFormatException e) {
                        existingComponent.setPrice(null);
                    }
                } else {
                    existingComponent.setPrice(null);
                }
            }

            if (requestData.containsKey("manufacturer")) {
                existingComponent.setManufacturer(requestData.get("manufacturer").toString());
            }

            if (requestData.containsKey("model")) {
                existingComponent.setModel(requestData.get("model").toString());
            }

            // Handle specifications - now it's a Map directly
            if (requestData.containsKey("specifications")) {
                Object specsObj = requestData.get("specifications");
                if (specsObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> specifications = (Map<String, Object>) specsObj;
                    existingComponent.setSpecifications(specifications);
                }
            }

            PcComponent updatedComponent = componentRepository.save(existingComponent);
            System.out.println("Component updated successfully: " + updatedComponent.getName());

            chromaDBService.syncComponent(updatedComponent);
            System.out.println("Updated component synced to ChromaDB: " + updatedComponent.getName());

            return ResponseEntity.ok(updatedComponent);

        } catch (Exception e) {
            System.err.println("Error updating component: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("{\"message\": \"Error updating component: " + e.getMessage() + "\"}");
        }
    }

    /**
     * Delete a component
     */
    @DeleteMapping("/components/{id}")
    public ResponseEntity<?> deleteComponent(@PathVariable Long id) {
        try {
            System.out.println("Deleting component with ID: " + id);

            // Check if component exists
            if (!componentRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }

            chromaDBService.deleteComponent(id);
            System.out.println("Component deleted from ChromaDB: " + id);

            componentRepository.deleteById(id);
            System.out.println("Component deleted successfully");

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            System.err.println("Error deleting component: " + e.getMessage());
            return ResponseEntity.internalServerError()
                    .body("{\"message\": \"Error deleting component: " + e.getMessage() + "\"}");
        }
    }

    /**
     * Get component by ID
     */
    @GetMapping("/components/{id}")
    public ResponseEntity<PcComponent> getComponentById(@PathVariable Long id) {
        try {
            Optional<PcComponent> component = componentRepository.findById(id);
            return component.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            System.err.println("Error fetching component: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Admin training chat - adds knowledge to ChromaDB
     */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> adminChat(@RequestBody ChatRequest request) {
        System.out.println("Admin training chat message: " + request.getMessage());
        System.out.println("User ID: " + request.getUserId());
        String response = pcBuilderService.getAdminTrainingResponse(request.getMessage(), request.getUserId());

        ChatResponse chatResponse = new ChatResponse(response);
        return ResponseEntity.ok(chatResponse);
    }
}