package com.example.aipcbuilder.service.build;

import com.example.aipcbuilder.dto.BuildWithComponents;
import com.example.aipcbuilder.model.Build;
import com.example.aipcbuilder.repository.BuildRepository;
import com.example.aipcbuilder.repository.PcComponentRepository;
import com.example.aipcbuilder.service.build.helper.BuildGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class BuildService {

    private final BuildRepository buildRepository;
    private final BuildGenerationService buildGenerationService;
    private final PcComponentRepository pcComponentRepository;

    public Build generateAndSaveBuild(Build build, Map<String, Map<String, Object>> requirements) {
        Build generatedBuild = buildGenerationService.generateBuild(build, requirements);
        return buildRepository.save(generatedBuild);
    }

    public List<BuildWithComponents> getUserBuilds(Long userId) {
        List<Build> builds = buildRepository.findByUserIdOrderByCreatedAtDesc(userId);
        log.debug("Found {} builds for user {}", builds.size(), userId);
        return builds.stream()
                .map(this::convertToBuildWithComponentsDTO)
                .collect(Collectors.toList());
    }

    private BuildWithComponents convertToBuildWithComponentsDTO(Build build) {
        return new BuildWithComponents(
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
        log.info("Deleted build {} for user {}", buildId, userId);
    }

}