package com.example.aipcbuilder.service;

import com.example.aipcbuilder.dto.BuildWithComponentsDTO;
import com.example.aipcbuilder.model.Build;
import com.example.aipcbuilder.repository.BuildRepository;
import com.example.aipcbuilder.repository.PcComponentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class BuildService {

    private final BuildRepository buildRepository;
    private final BuildGenerationService buildGenerationService;
    private final PcComponentRepository pcComponentRepository;

    public BuildService(BuildRepository buildRepository, BuildGenerationService buildGenerationService, PcComponentRepository pcComponentRepository) {
        this.buildRepository = buildRepository;
        this.buildGenerationService = buildGenerationService;
        this.pcComponentRepository = pcComponentRepository;
    }

    public Build generateAndSaveBuild(Build build, Map<String, Map<String, Object>> requirements) {
        // Generate components based on requirements (requirements are passed separately)
        Build generatedBuild = buildGenerationService.generateBuild(build, requirements);

        // Save the complete build with component IDs (without storing requirements)
        return buildRepository.save(generatedBuild);
    }

    public List<BuildWithComponentsDTO> getUserBuilds(Long userId) {
        List<Build> builds = buildRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return builds.stream()
                .map(this::convertToBuildWithComponentsDTO)
                .collect(Collectors.toList());
    }

    private BuildWithComponentsDTO convertToBuildWithComponentsDTO(Build build) {
        return new BuildWithComponentsDTO(
                build.getId(),
                build.getUserId(),
                build.getName(),
                build.getTotalPrice().doubleValue(),
                build.getCreatedAt(),
                getComponentName(build.getCpuId()),
                getComponentName(build.getGpuId()),
                getComponentName(build.getPsuId()),
                getComponentName(build.getRamId()),
                getComponentName(build.getStorageId()),
                getComponentName(build.getMotherboardId()),
                getComponentName(build.getCaseId())
        );
    }

    private String getComponentName(Long componentId) {
        if (componentId == null) {
            return "Not selected";
        }

        return pcComponentRepository.findComponentNameById(componentId)
                .orElse("Component not found");
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