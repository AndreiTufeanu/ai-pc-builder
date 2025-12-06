package com.example.aipcbuilder.service.build.helper;

import com.example.aipcbuilder.model.PcComponent;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class BuildResultMapperService {

    public Map<String, Long> convertComponentsToIdMap(Map<String, PcComponent> selectedComponents) {
        Map<String, Long> componentIds = new HashMap<>();
        for (Map.Entry<String, PcComponent> entry : selectedComponents.entrySet()) {
            String componentKey = entry.getKey();
            PcComponent component = entry.getValue();
            if (component != null) {
                switch (componentKey) {
                    case "cpu": componentIds.put("cpuId", component.getId()); break;
                    case "gpu": componentIds.put("gpuId", component.getId()); break;
                    case "psu": componentIds.put("psuId", component.getId()); break;
                    case "ram": componentIds.put("ramId", component.getId()); break;
                    case "storage": componentIds.put("storageId", component.getId()); break;
                    case "motherboard": componentIds.put("motherboardId", component.getId()); break;
                    case "case": componentIds.put("caseId", component.getId()); break;
                }
            }
        }
        return componentIds;
    }

    public double calculateTotalPrice(Map<String, PcComponent> selectedComponents) {
        double totalPrice = 0.0;
        for (PcComponent component : selectedComponents.values()) {
            if (component != null && component.getPrice() != null) {
                totalPrice += component.getPrice().doubleValue();
            }
        }
        return totalPrice;
    }
}