package com.example.aipcbuilder.service.component;

import com.example.aipcbuilder.model.PcComponent;
import com.example.aipcbuilder.repository.PcComponentRepository;
import com.example.aipcbuilder.service.chroma.ChromaDBService;
import com.example.aipcbuilder.utils.ComponentUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class ComponentService {

    private final PcComponentRepository componentRepository;
    private final ChromaDBService chromaDBService;
    private final ComponentUtils componentUtils;

    public List<PcComponent> getAllComponents() {
        return componentRepository.findAllByOrderByTypeManufacturerId();
    }

    public Optional<PcComponent> getComponentById(Long id) {
        return componentRepository.findById(id);
    }

    public PcComponent createComponent(Map<String, Object> requestData) {
        if (!componentUtils.isValidComponentRequest(requestData)) {
            throw new IllegalArgumentException("Component name and type are required");
        }

        PcComponent component = componentUtils.createComponentFromRequest(requestData);
        PcComponent savedComponent = componentRepository.save(component);

        // Sync with ChromaDB
        chromaDBService.syncComponent(savedComponent);

        return savedComponent;
    }

    public Optional<PcComponent> updateComponent(Long id, Map<String, Object> requestData) {
        return componentRepository.findById(id)
                .map(existingComponent -> {
                    componentUtils.updateComponentFromRequest(existingComponent, requestData);
                    PcComponent updatedComponent = componentRepository.save(existingComponent);

                    chromaDBService.syncComponent(updatedComponent);

                    return updatedComponent;
                });
    }

    public boolean deleteComponent(Long id) {
        if (!componentRepository.existsById(id)) {
            return false;
        }

        chromaDBService.deleteComponent(id);
        componentRepository.deleteById(id);

        return true;
    }
}