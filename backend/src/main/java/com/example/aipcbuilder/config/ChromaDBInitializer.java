package com.example.aipcbuilder.config;

import com.example.aipcbuilder.model.PcComponent;
import com.example.aipcbuilder.repository.PcComponentRepository;
import com.example.aipcbuilder.service.ChromaDBService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChromaDBInitializer implements CommandLineRunner {

    private final PcComponentRepository componentRepository;
    private final ChromaDBService chromaDBService;

    public ChromaDBInitializer(PcComponentRepository componentRepository,
                               ChromaDBService chromaDBService) {
        this.componentRepository = componentRepository;
        this.chromaDBService = chromaDBService;
    }

    @Override
    public void run(String... args) throws Exception {
        // Sync all components on startup
        List<PcComponent> components = componentRepository.findAll();
        if (!components.isEmpty()) {
            System.out.println("Syncing " + components.size() + " components to ChromaDB...");
            chromaDBService.syncAllComponents(components);

            String status = chromaDBService.getChromaDbStatus();
            System.out.println("ChromaDB connection status: " + status);
        }
    }
}