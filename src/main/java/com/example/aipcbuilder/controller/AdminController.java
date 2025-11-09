package com.example.aipcbuilder.controller;

import com.example.aipcbuilder.dto.ChatRequest;
import com.example.aipcbuilder.dto.ChatResponse;
import com.example.aipcbuilder.model.PcComponent;
import com.example.aipcbuilder.repository.PcComponentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final PcComponentRepository componentRepository;

    public AdminController(PcComponentRepository componentRepository) {
        this.componentRepository = componentRepository;
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
    public ResponseEntity<?> addComponent(@RequestBody PcComponent component) {
        try {
            System.out.println("Adding new component: " + component.getName());
            System.out.println("Type: " + component.getType());
            System.out.println("Price: " + component.getPrice());
            System.out.println("Manufacturer: " + component.getManufacturer());
            System.out.println("Model: " + component.getModel());

            // Basic validation
            if (component.getName() == null || component.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Component name is required");
            }

            if (component.getType() == null) {
                return ResponseEntity.badRequest().body("Component type is required");
            }

            // Check if component already exists
            if (component.getModel() != null && !component.getModel().trim().isEmpty()) {
                boolean exists = componentRepository.existsByNameAndModel(component.getName(), component.getModel());
                if (exists) {
                    return ResponseEntity.badRequest().body("Component with this name and model already exists");
                }
            }

            // Save to database
            PcComponent savedComponent = componentRepository.save(component);
            System.out.println("Component saved with ID: " + savedComponent.getId());

            return ResponseEntity.ok(savedComponent);

        } catch (Exception e) {
            System.err.println("Error adding component: " + e.getMessage());
            return ResponseEntity.internalServerError()
                    .body("Error adding component: " + e.getMessage());
        }
    }

    /**
     * Update an existing component
     */
    @PutMapping("/components/{id}")
    public ResponseEntity<?> updateComponent(@PathVariable Long id, @RequestBody PcComponent component) {
        try {
            System.out.println("Updating component with ID: " + id);

            // Check if component exists
            Optional<PcComponent> existingComponent = componentRepository.findById(id);
            if (existingComponent.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Update the existing component
            PcComponent componentToUpdate = existingComponent.get();
            componentToUpdate.setName(component.getName());
            componentToUpdate.setType(component.getType());
            componentToUpdate.setDescription(component.getDescription());
            componentToUpdate.setPrice(component.getPrice());
            componentToUpdate.setManufacturer(component.getManufacturer());
            componentToUpdate.setModel(component.getModel());
            componentToUpdate.setSpecifications(component.getSpecifications());

            PcComponent updatedComponent = componentRepository.save(componentToUpdate);
            System.out.println("Component updated successfully: " + updatedComponent.getName());

            return ResponseEntity.ok(updatedComponent);

        } catch (Exception e) {
            System.err.println("Error updating component: " + e.getMessage());
            return ResponseEntity.internalServerError()
                    .body("Error updating component: " + e.getMessage());
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

            componentRepository.deleteById(id);
            System.out.println("Component deleted successfully");

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            System.err.println("Error deleting component: " + e.getMessage());
            return ResponseEntity.internalServerError()
                    .body("Error deleting component: " + e.getMessage());
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
        // TODO: Implement admin training chat with ChromaDB integration
        // For now, return a stub response

        System.out.println("Admin training chat message: " + request.getMessage());
        System.out.println("User ID: " + request.getUserId());

        String response = "Thank you for the training information! This will be added to the knowledge base. ";
        response += "When this is connected to ChromaDB, your input about PC components will help improve recommendations for all users.";

        ChatResponse chatResponse = new ChatResponse(response);
        return ResponseEntity.ok(chatResponse);
    }
}