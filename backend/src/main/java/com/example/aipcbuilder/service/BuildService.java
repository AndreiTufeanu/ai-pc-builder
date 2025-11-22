package com.example.aipcbuilder.service;

import com.example.aipcbuilder.model.Build;
import com.example.aipcbuilder.repository.BuildRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class BuildService {

    private final BuildRepository buildRepository;
    private final BuildGenerationService buildGenerationService;

    public BuildService(BuildRepository buildRepository, BuildGenerationService buildGenerationService) {
        this.buildRepository = buildRepository;
        this.buildGenerationService = buildGenerationService;
    }

    public Build generateAndSaveBuild(Build build, Map<String, Map<String, Object>> requirements) {
        // Generate components based on requirements (requirements are passed separately)
        Build generatedBuild = buildGenerationService.generateBuild(build, requirements);

        // Save the complete build with component IDs (without storing requirements)
        return buildRepository.save(generatedBuild);
    }

    public List<Build> getUserBuilds(Long userId) {
        return buildRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public void deleteBuild(Long userId, Long buildId) {
        buildRepository.deleteByUserIdAndId(userId, buildId);
    }

    public Build getBuild(Long userId, Long buildId) {
        return buildRepository.findById(buildId)
                .filter(build -> build.getUserId().equals(userId))
                .orElse(null);
    }
}