package com.example.aipcbuilder.controller;

import com.example.aipcbuilder.dto.BuildRequest;
import com.example.aipcbuilder.dto.BuildResponse;
import com.example.aipcbuilder.dto.BuildWithComponents;
import com.example.aipcbuilder.model.Build;
import com.example.aipcbuilder.service.build.BuildService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/build")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class BuildController {

    private final BuildService buildService;

    @PostMapping("/generate")
    public ResponseEntity<BuildResponse> generateBuild(@Valid @RequestBody BuildRequest request) {
        try {
            System.out.println("Generating build for user: " + request.getUserId());
            System.out.println("Name: " + request.getName());
            System.out.println("Budget: " + request.getBudget());
            System.out.println("Requirements: " + request.getRequirements());

            // Create basic build (without requirements)
            Build build = new Build(
                    request.getUserId(),
                    request.getName(),
                    request.getDescription(),
                    request.getBudget()
            );

            // Generate and save the complete build with components
            // Pass requirements separately for generation only
            Build generatedBuild = buildService.generateAndSaveBuild(build, request.getRequirements());

            BuildResponse response = new BuildResponse(
                    generatedBuild,
                    "Build generated successfully!",
                    true
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Error generating build: " + e.getMessage());
            BuildResponse errorResponse = new BuildResponse(
                    null,
                    "Error generating build: " + e.getMessage(),
                    false
            );
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/user/{userId}/builds")
    public List<BuildWithComponents> getUserBuilds(@PathVariable Long userId) {
        return buildService.getUserBuilds(userId);
    }

    @DeleteMapping("/{buildId}/user/{userId}")
    public ResponseEntity<Void> deleteBuild(@PathVariable Long userId, @PathVariable Long buildId) {
        try {
            buildService.deleteBuild(userId, buildId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("Error deleting build: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}