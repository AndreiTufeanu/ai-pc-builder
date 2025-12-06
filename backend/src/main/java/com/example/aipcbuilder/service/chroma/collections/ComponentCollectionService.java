package com.example.aipcbuilder.service.chroma.collections;

import com.example.aipcbuilder.model.PcComponent;
import com.example.aipcbuilder.service.chroma.client.ChromaDBClient;
import com.example.aipcbuilder.service.chroma.helper.ChromaDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.core.ParameterizedTypeReference;

@Service
@Slf4j
@RequiredArgsConstructor
public class ComponentCollectionService {

    private static final String COLLECTION_NAME = "components";
    private static final String UPSERT_ENDPOINT = "/components/upsert";
    private static final String SEARCH_ENDPOINT = "/search";
    private static final String DELETE_ENDPOINT = "/components/";

    private final ChromaDBClient client;
    private final ChromaDataService dataHelper;

    public void upsert(List<PcComponent> components) {
        List<Map<String, Object>> componentsData = components.stream()
                .map(dataHelper::createComponentData)
                .collect(Collectors.toList());

        try {
            client.post(UPSERT_ENDPOINT, componentsData, String.class);
            log.info("Upserted {} components to ChromaDB", components.size());
        } catch (Exception e) {
            log.error("Failed to upsert components: {}", e.getMessage());
        }
    }

    public void upsert(PcComponent component) {
        upsert(List.of(component));
    }

    public void delete(Long componentId) {
        try {
            client.delete(DELETE_ENDPOINT + componentId);
            log.info("Deleted component {} from ChromaDB", componentId);
        } catch (Exception e) {
            log.error("Failed to delete component {}: {}", componentId, e.getMessage());
        }
    }

    public List<Map<String, Object>> search(String query, int limit, String componentType) {
        Map<String, Object> searchRequest = new HashMap<>();
        searchRequest.put("query", query);
        searchRequest.put("collection", COLLECTION_NAME);
        searchRequest.put("n_results", limit);

        if (componentType != null && !componentType.trim().isEmpty()) {
            searchRequest.put("where", Map.of("component_type", componentType.toUpperCase()));
        }

        try {
            Map<String, Object> response = client.post(
                    SEARCH_ENDPOINT,
                    searchRequest,
                    new ParameterizedTypeReference<>() {}
            );

            return (List<Map<String, Object>>) response.getOrDefault("results", List.of());
        } catch (Exception e) {
            log.error("Failed to search components: {}", e.getMessage());
            return List.of();
        }
    }
}